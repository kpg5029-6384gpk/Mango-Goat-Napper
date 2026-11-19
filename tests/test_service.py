import sys
from pathlib import Path
import pytest
from aiohttp.test_utils import TestClient, TestServer
sys.path.insert(0, str(Path(__file__).parents[1] / 'companion'))
from service import Service

@pytest.mark.asyncio
async def test_local_feed_auth_and_reset():
    service = Service('secret')
    client = TestClient(TestServer(service.app()))
    await client.start_server()
    try:
        headers = {'Host': '127.0.0.1:18765', 'Authorization': 'Bearer secret'}
        assert (await client.get('/status', headers={'Host':'127.0.0.1:18765'})).status == 401
        assert (await client.get('/status', headers={**headers, 'Origin':'https://example.com'})).status == 403
        response = await client.get('/status', headers=headers)
        body = await response.json()
        assert body['status'] == 'Disconnected' and body['pets']['sequence'] == 0
        assert body['cooldownSeconds'] == 300
        assert service.last_poll is not None
        service.feed.test()
        body = await (await client.get('/status', headers=headers)).json()
        assert body['status'] == 'Disconnected'
        assert body['pets']['events'][0]['test'] is True
        assert body['pets']['events'][0]['username'] == 'TestViewer'
        assert (await client.post('/control', headers=headers, json={'action':'disconnect'})).status == 200
        assert (await client.post('/control', headers=headers, json={'action':'connect','username':'bad name'})).status == 400
    finally:
        await client.close()

