import pytest
import numpy as np
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from feature_extraction import extract_profile_features, extract_behavioral_features

def test_extract_profile_features_empty():
    item = {"profile": {}}
    res = extract_profile_features(item)
    assert res["profile_pic"] == 0
    assert res["nums/length_username"] == 0.0
    assert res["fullname_words"] == 0
    assert res["nums/length_fullname"] == 0.0
    assert res["name==username"] == 0
    assert res["description_length"] == 0
    assert res["external_URL"] == 0
    assert res["private"] == 0
    assert res["posts"] == 0
    assert res["followers"] == 0
    assert res["following"] == 0

def test_extract_profile_features_complete():
    item = {
        "profile": {
            "username": "user123",
            "fullName": "User Name",
            "biography": "Hello World",
            "profilePicUrl": "http://example.com/pic.jpg",
            "externalUrl": "http://example.com",
            "isPrivate": True,
            "mediaCount": 50,
            "followerCount": 100,
            "followingCount": 150
        }
    }
    res = extract_profile_features(item)
    assert res["profile_pic"] == 1
    assert res["nums/length_username"] == 3 / 7
    assert res["fullname_words"] == 2
    assert res["nums/length_fullname"] == 0.0
    assert res["name==username"] == 0
    assert res["description_length"] == 11
    assert res["external_URL"] == 1
    assert res["private"] == 1
    assert res["posts"] == 50
    assert res["followers"] == 100
    assert res["following"] == 150

def test_extract_profile_features_alternative_keys():
    item = {
        "profile": {
            "username": "user123",
            "full_name": "user123",
            "profile_pic_url": "exists",
            "external_url": "exists",
            "private": True,
            "postsCount": 10,
            "followersCount": 20,
            "followingCount": 30
        }
    }
    res = extract_profile_features(item)
    assert res["profile_pic"] == 1
    assert res["name==username"] == 1
    assert res["external_URL"] == 1
    assert res["private"] == 1
    assert res["posts"] == 10
    assert res["followers"] == 20
    assert res["following"] == 30

def test_extract_profile_features_string_numeric():
    # Edge case: scraper returns strings instead of ints
    item = {
        "profile": {
            "postsCount": "10",
            "followersCount": "20",
            "followingCount": "30"
        }
    }
    res = extract_profile_features(item)
    assert res["posts"] == 10
    assert res["followers"] == 20
    assert res["following"] == 30

def test_extract_behavioral_features_empty():
    item = {"comments": []}
    res = extract_behavioral_features(item)
    assert res.shape == (10, 5)
    assert np.all(res == 0)

def test_extract_behavioral_features_sorting_and_gaps():
    item = {
        "comments": [
            {"text": "second", "timestamp": 100, "likes": 5},
            {"text": "first", "timestamp": 50, "likes": 2},
            {"text": "third", "timestamp": 120, "likes": 10}
        ]
    }
    res = extract_behavioral_features(item, seq_length=3)
    assert res.shape == (3, 5)
    # first should be first
    assert res[0, 0] == 5 # len("first")
    assert res[0, 1] == 0 # gap
    assert res[0, 2] == 2 # likes
    
    # second
    assert res[1, 0] == 6 # len("second")
    assert res[1, 1] == 50 # gap 100 - 50
    assert res[1, 2] == 5 # likes

    # third
    assert res[2, 0] == 5 # len("third")
    assert res[2, 1] == 20 # gap 120 - 100
    assert res[2, 2] == 10 # likes

def test_extract_behavioral_features_iso_timestamp():
    item = {
        "comments": [
            {"text": "hello", "timestamp": "2023-10-01T12:00:00Z", "likesCount": 1}
        ]
    }
    res = extract_behavioral_features(item)
    assert res[0, 0] == 5
    assert res[0, 1] == 0 
    assert res[0, 2] == 1
    assert res[0, 3] == 1

def test_extract_behavioral_features_malformed_timestamp():
    item = {
        "comments": [
            {"text": "hello", "timestamp": "invalid_date", "likesCount": 1}
        ]
    }
    res = extract_behavioral_features(item)
    assert res[0, 0] == 5
    assert res[0, 1] == 0 
    assert res[0, 2] == 1

def test_extract_behavioral_features_missing_fields():
    item = {
        "comments": [
            {}  # completely empty comment
        ]
    }
    res = extract_behavioral_features(item)
    assert res[0, 0] == 0 # len("")
    assert res[0, 1] == 0 # gap
    assert res[0, 2] == 0 # likes
    assert res[0, 3] == 1 # is_comment
