import numpy as np
import re

def extract_profile_features(item: dict) -> dict:
    """
    Extracts 11 Kaggle features from Apify profile data.
    Input item format: { "profile": { ... } }
    """
    p = item.get("profile", {})
    
    username = str(p.get("username", ""))
    full_name = str(p.get("fullName", p.get("full_name", "")))
    biography = str(p.get("biography", ""))
    
    # 1. profile_pic (0 if default/null, 1 if exists)
    # Most scrapers return a URL or null
    profile_pic = 1 if p.get("profilePicUrl") or p.get("profile_pic_url") else 0
    
    # 2. nums/length_username
    digit_count_user = sum(c.isdigit() for c in username)
    nums_length_username = digit_count_user / len(username) if username else 0
    
    # 3. fullname_words
    fullname_words = len(full_name.split())
    
    # 4. nums/length_fullname
    digit_count_full = sum(c.isdigit() for c in full_name)
    nums_length_fullname = digit_count_full / len(full_name) if full_name else 0
    
    # 5. name==username (1 if same, 0 otherwise)
    name_equals_username = 1 if username.lower() == full_name.lower() and username else 0
    
    # 6. description_length
    description_length = len(biography)
    
    # 7. external_URL (1 if exists, 0 otherwise)
    external_URL = 1 if p.get("externalUrl") or p.get("external_url") else 0
    
    # 8. private (1 if true, 0 otherwise)
    private = 1 if p.get("isPrivate") or p.get("private") else 0
    
    # 9. posts
    posts = int(p.get("mediaCount", p.get("postsCount", p.get("posts_count", 0))))
    
    # 10. followers
    followers = int(p.get("followerCount", p.get("followersCount", p.get("followers_count", 0))))
    
    # 11. following
    following = int(p.get("followingCount", p.get("followingCount", p.get("following_count", 0))))
    
    return {
        "profile_pic": profile_pic,
        "nums/length_username": nums_length_username,
        "fullname_words": fullname_words,
        "nums/length_fullname": nums_length_fullname,
        "name==username": name_equals_username,
        "description_length": description_length,
        "external_URL": external_URL,
        "private": private,
        "posts": posts,
        "followers": followers,
        "following": following
    }

def extract_behavioral_features(item: dict, seq_length: int = 10) -> np.ndarray:
    """
    Extracts sequence features for LSTM from Apify comment/like data.
    Input item format: { "comments": [ ... ], "likes": [ ... ] }
    Returns: (seq_length, 5) array
    """
    comments = item.get("comments", [])
    # Sort comments by timestamp
    # Apify usually returns them latest first, we want chronological
    try:
        sorted_comments = sorted(comments, key=lambda x: x.get("timestamp", 0))
    except:
        sorted_comments = comments

    features = np.zeros((seq_length, 5))
    
    last_t = 0
    for i in range(min(len(sorted_comments), seq_length)):
        c = sorted_comments[i]
        
        # Handle different timestamp formats (string ISO vs unix int)
        t = c.get("timestamp", 0)
        if isinstance(t, str):
            # Very basic parse if ISO string, in production use dateutil
            try:
                from datetime import datetime
                t = datetime.fromisoformat(t.replace('Z', '+00:00')).timestamp()
            except:
                t = 0
        
        gap = t - last_t if last_t > 0 else 0
        last_t = t
        
        text = str(c.get("text", ""))
        likes = int(c.get("likesCount", c.get("likes", 0)))
        
        features[i, 0] = len(text)
        features[i, 1] = gap
        features[i, 2] = likes
        features[i, 3] = 1 # is_comment
        features[i, 4] = 0 # pad
        
    return features
