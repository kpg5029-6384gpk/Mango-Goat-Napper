"""TikTok LIVE connection and local authenticated gift feed."""
import asyncio
import contextlib
import logging
import time
from aiohttp import web
from TikTokLive import TikTokLiveClient
from TikTokLive.events import ConnectEvent, GiftEvent, LiveEndEvent
from TikTokLive.client.errors import UserOfflineError
from pet_gifts import PetGifts
from core import normalize, backoff

LOG = logging.getLogger('MangoGoatNapper')

class Service:
    def __init__(self, token):
        self.token = token
        self.feed = PetGifts()
        self.status = 'Disconnected'
        self.message = ''
        self.username = ''
        self.client = None
        self.task = None
        self.last_poll = None

    async def stop(self):
        if self.client:
            with contextlib.suppress(Exception):
                await self.client.disconnect(close_client=True)
        if self.task:
            self.task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self.task
        self.client = self.task = None
        self.status = 'Disconnected'

    async def watch(self):
        attempt = 0
        while True:
            self.status = 'Connecting' if not attempt else 'Reconnecting'
            client = self.client = TikTokLiveClient(unique_id=self.username)
            @client.on(ConnectEvent)
            async def connected(event):
                nonlocal attempt
                attempt = 0
                self.status = 'Live'
                self.message = ''
            @client.on(GiftEvent)
            async def gift(event):
                if self.status == 'Live':
                    try:
                        self.feed.event(event)
                    except (ValueError, AttributeError, TypeError):
                        LOG.warning('Rejected malformed gift event')
            @client.on(LiveEndEvent)
            async def ended(event):
                self.status = 'Offline'
                await client.disconnect()
            try:
                task = await client.start(fetch_gift_info=True, fetch_room_info=False)
                await task
            except UserOfflineError:
                self.status = 'Offline'
                self.message = 'Creator is offline; retrying automatically'
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                self.status = 'Error'
                self.message = f'{type(exc).__name__}: TikTok unavailable; retrying automatically'
                LOG.warning('TikTok connection failed: %s', type(exc).__name__)
            finally:
                with contextlib.suppress(Exception):
                    await client.disconnect(close_client=True)
            attempt += 1
            await asyncio.sleep(backoff(attempt))

    def app(self):
        @web.middleware
        async def security(request, handler):
            if request.headers.get('Origin') or request.headers.get('Host') != '127.0.0.1:18765':
                raise web.HTTPForbidden()
            if request.headers.get('Authorization') != 'Bearer ' + self.token:
                raise web.HTTPUnauthorized()
            return await handler(request)
        app = web.Application(middlewares=[security], client_max_size=2048)
        async def status(request):
            self.last_poll = time.monotonic()
            return web.json_response(dict(status=self.status, message=self.message,
                                          username=self.username, pets=self.feed.snapshot(),
                                          lastGift=self.feed.last_result))
        async def control(request):
            try:
                data = await request.json()
                if not isinstance(data, dict) or data.get('action') not in ('connect', 'disconnect'):
                    raise ValueError('Invalid action')
                username = normalize(data.get('username')) if data['action'] == 'connect' else ''
            except (ValueError, TypeError) as exc:
                return web.json_response({'error': str(exc)}, status=400)
            await self.stop()
            self.feed = PetGifts()
            self.username = username
            if username:
                self.task = asyncio.create_task(self.watch())
            return web.json_response({'status': self.status})
        async def cleanup(app):
            await self.stop()
        app.router.add_get('/status', status)
        app.router.add_post('/control', control)
        app.on_cleanup.append(cleanup)
        return app
