"""Qualifying TikTok LIVE gift feed for Mango Goat Napper."""
import re
import time
import uuid
from collections import OrderedDict, deque


class PetGifts:
    def __init__(self):
        self.session = str(uuid.uuid4())
        self.sequence = 0
        self.events = deque(maxlen=256)
        self.seen = OrderedDict()
        self.last_result = 'No gifts received yet'

    def test(self, now=None):
        """Send an explicitly marked local test without requiring a LIVE."""
        now = time.time() if now is None else now
        self.sequence += 1
        self.events.append(dict(sequence=self.sequence, created=now,
                                unitCoins=200, username='TestViewer', test=True))
        self.last_result = 'Test goat sent to Minecraft'

    def event(self, event, now=None):
        now = time.time() if now is None else now
        gift = getattr(event, 'gift', None)
        if gift is None or getattr(event, 'streaking', True):
            self.last_result = 'Gift missing details or streak still in progress'
            return False
        value = getattr(gift, 'diamond_count', None)
        gift_id = getattr(event, 'gift_id', None)
        user = getattr(event, 'user', None)
        name = getattr(user, 'unique_id', None)
        if type(value) is not int or value != 200:
            self.last_result = f'Gift value: {value!s}; requires 200 coins per gift'
            return False
        if str(getattr(gift, 'name', '')).strip().casefold() != 'goat':
            self.last_result = 'Gift rejected: not the Goat gift'
            return False
        if type(gift_id) is not int or gift_id <= 0:
            self.last_result = 'Gift rejected: missing gift ID'
            return False
        if not isinstance(name, str) or not re.fullmatch(r'[A-Za-z0-9._]{1,24}', name):
            self.last_result = 'Gift rejected: sender username unavailable'
            return False
        group = getattr(event, 'group_id', 0)
        user_id = getattr(user, 'id', 0)
        message = getattr(getattr(event, 'common', None), 'msg_id', 0)
        key = ('group', str(user_id), str(group), gift_id) if group and user_id else ('message', str(message)) if message else None
        if key is None or key in self.seen:
            self.last_result = 'Gift rejected: duplicate or missing event ID'
            return False
        self.seen[key] = now
        while len(self.seen) > 2048:
            self.seen.popitem(last=False)
        self.sequence += 1
        self.events.append(dict(sequence=self.sequence, created=now, unitCoins=value, username=name))
        self.last_result = f'Accepted {value}-coin gift from @{name}'
        return True

    def snapshot(self, now=None):
        now = time.time() if now is None else now
        return dict(session=self.session, sequence=self.sequence,
                    events=[e for e in self.events if 0 <= now - e['created'] <= 30])
