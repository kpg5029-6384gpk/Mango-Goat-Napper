from types import SimpleNamespace as N
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).parents[1] / 'companion'))
from pet_gifts import PetGifts

def gift(name='Goat', coins=200, msg=1, **kw):
    data = dict(gift=N(name=name, diamond_count=coins), gift_id=123, streaking=False,
                user=N(id=42, unique_id='viewer'), group_id=0, common=N(msg_id=msg))
    data.update(kw)
    return N(**data)

def test_only_completed_goat_at_expected_price():
    feed = PetGifts()
    assert not feed.event(gift(name='Rose'), 100)
    assert not feed.event(gift(coins=500), 100)
    assert not feed.event(gift(streaking=True), 100)
    assert feed.event(gift(), 100)
    assert not feed.event(gift(), 101)
    assert len(feed.snapshot(100)['events']) == 1
    assert not feed.snapshot(131)['events']

def test_test_event():
    feed = PetGifts()
    feed.test(100)
    assert feed.snapshot(100)['events'][0]['test'] is True

def test_tiktoklive_gift_object():
    from TikTokLive.events import GiftEvent
    from TikTokLive.proto.custom_proto import ExtendedGift, ExtendedUser
    from TikTokLiveProto.v3.webcast.shared.message import CommonMessageData
    event = GiftEvent(gift_id=123, gift=ExtendedGift(name='Goat', diamond_count=200, type=1),
                      user=ExtendedUser(id=42, display_id='gift.sender'),
                      common=CommonMessageData(msg_id=500), repeat_count=1, repeat_end=1)
    assert PetGifts().event(event, 100)
