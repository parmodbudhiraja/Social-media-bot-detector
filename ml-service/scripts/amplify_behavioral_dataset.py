"""
Amplifies the behavioral seed from JSON into a CSV with the correct format:
userid, List of Comment-level data, List of Like-level data, Is a bot?

Then uses CTGAN on extracted numerical features to generate new synthetic rows,
and reconstructs them back into the proper CSV format.
"""
import pandas as pd
import numpy as np
import json
import os
import random
import string
from ctgan import CTGAN

# Bot and human comment/like templates for reconstruction
BOT_COMMENTS_POOL = [
    "Nice pic! 🔥", "Check my profile!", "DM me for collab 💯", "Amazing! ❤️",
    "Follow me back!", "Love this! 😍", "Great content! 👏", "Wow! 🙌",
    "Beautiful! ✨", "DM for promotions!", "Nice! 🔥🔥🔥", "Check bio for link!",
    "Cool! 😎", "Follow for follow! 🤝", "Love it! ❤️❤️", "Great post! 💯",
    "Awesome! 🙏", "Like for like!", "So good! 👌", "Check out my page!",
    "🔥🔥🔥", "❤️❤️❤️", "👏👏", "💯💯", "😍😍😍",
]

HUMAN_COMMENTS_POOL = [
    "This reminds me of my trip to Italy last summer, such beautiful colors!",
    "I've been following your photography for a while now, your composition has really improved",
    "Where did you get that outfit? The style is perfect for spring",
    "The lighting in this shot is incredible, what camera are you using?",
    "I tried making this recipe yesterday and it turned out amazing, thanks for sharing!",
    "This is such an important message, more people need to hear this",
    "Your dog is adorable! What breed is it?",
    "Can't believe how much this place has changed since I was there",
    "The sunset in the background really makes this photo stand out",
    "I love how you edited this, the tones are so warm and inviting",
    "This is exactly the kind of content I needed to see today, thank you",
    "Your travel recommendations have been spot on, adding this to my list",
    "The details in this artwork are mind-blowing, how long did it take?",
    "I completely agree with your take on this, well said",
    "This cafe looks amazing, is it in the downtown area?",
    "Your fitness journey is truly inspiring, keep it up!",
    "The way you captured the reflection in the water is stunning",
    "Just ordered the same book after seeing your review, can't wait to read it",
    "Such a thoughtful caption, really resonated with me",
    "Happy birthday! Hope you had an amazing celebration 🎂",
    "This view is breathtaking, nature never ceases to amaze",
    "Finally someone who understands the struggle of morning workouts haha",
    "Love seeing your creative process, the behind-the-scenes content is great",
    "This hiking trail looks challenging but worth it for that view!",
]

def extract_features_from_seed():
    """Extract numerical features from the JSON seed for CTGAN training."""
    if not os.path.exists('data/behavioral_seed.json'):
        raise FileNotFoundError("Run generate_behavioral_seed.py first.")

    with open('data/behavioral_seed.json', 'r') as f:
        users = json.load(f)

    print(f"Extracting features from {len(users)} seed users...")
    rows = []
    for u in users:
        comments = u.get("comments", [])
        likes = u.get("likes", [])

        # Extract numerical features that CTGAN can learn distributions from
        num_comments = len(comments)
        num_likes = len(likes)

        # Comment text lengths
        comment_lengths = [len(c.get("text", "")) for c in comments]
        avg_comment_len = np.mean(comment_lengths) if comment_lengths else 0
        std_comment_len = np.std(comment_lengths) if len(comment_lengths) > 1 else 0

        # Comment time gaps
        comment_timestamps = sorted([c.get("timestamp", 0) for c in comments])
        comment_gaps = [comment_timestamps[i+1] - comment_timestamps[i] for i in range(len(comment_timestamps)-1)]
        avg_comment_gap = np.mean(comment_gaps) if comment_gaps else 0
        std_comment_gap = np.std(comment_gaps) if len(comment_gaps) > 1 else 0
        min_comment_gap = min(comment_gaps) if comment_gaps else 0

        # Comment likes received
        comment_likes = [c.get("likes_on_comment", c.get("likes", 0)) for c in comments]
        avg_comment_likes = np.mean(comment_likes) if comment_likes else 0

        # Like time gaps
        like_timestamps = sorted([l.get("timestamp", 0) for l in likes])
        like_gaps = [like_timestamps[i+1] - like_timestamps[i] for i in range(len(like_timestamps)-1)]
        avg_like_gap = np.mean(like_gaps) if like_gaps else 0
        std_like_gap = np.std(like_gaps) if len(like_gaps) > 1 else 0
        min_like_gap = min(like_gaps) if like_gaps else 0

        # Unique posts
        unique_commented_posts = len(set(c.get("postid", "") for c in comments))
        unique_liked_posts = len(set(l.get("postid", "") for l in likes))

        rows.append({
            "is_bot": 1 if u.get("is_bot") else 0,
            "num_comments": num_comments,
            "num_likes": num_likes,
            "avg_comment_len": avg_comment_len,
            "std_comment_len": std_comment_len,
            "avg_comment_gap": avg_comment_gap,
            "std_comment_gap": std_comment_gap,
            "min_comment_gap": min_comment_gap,
            "avg_comment_likes": avg_comment_likes,
            "avg_like_gap": avg_like_gap,
            "std_like_gap": std_like_gap,
            "min_like_gap": min_like_gap,
            "unique_commented_posts": unique_commented_posts,
            "unique_liked_posts": unique_liked_posts,
        })

    return pd.DataFrame(rows)


def random_username(is_bot: bool) -> str:
    if is_bot:
        patterns = [
            f"user_{random.randint(100000, 999999)}",
            f"{''.join(random.choices(string.ascii_lowercase, k=4))}{random.randint(1000, 9999)}",
            f"follow_{''.join(random.choices(string.ascii_lowercase, k=3))}_{random.randint(10, 99)}",
            f"promo_{''.join(random.choices(string.ascii_lowercase, k=5))}",
            f"{''.join(random.choices(string.ascii_lowercase + string.digits, k=12))}",
        ]
    else:
        first_names = ["emma", "liam", "olivia", "noah", "ava", "james", "sophia", "lucas",
                       "mia", "alex", "sarah", "marcus", "julia", "david", "nina", "carlos"]
        patterns = [
            f"{random.choice(first_names)}_{random.choice(['photo', 'travel', 'art', 'life'])}",
            f"{random.choice(first_names)}.{random.choice(first_names)[:3]}",
            f"the.real.{random.choice(first_names)}",
            f"{random.choice(first_names)}_{random.randint(90, 99)}",
        ]
    return random.choice(patterns)


def reconstruct_row(features: dict) -> dict:
    """
    Takes CTGAN-generated numerical features and reconstructs a full CSV row
    with userid, JSON comment list, JSON like list, and is_bot boolean.
    """
    is_bot = bool(round(features["is_bot"]))
    num_comments = max(1, int(round(features["num_comments"])))
    num_likes = max(1, int(round(features["num_likes"])))
    avg_gap = max(1, abs(features["avg_comment_gap"]))
    avg_like_gap_val = max(1, abs(features["avg_like_gap"]))
    avg_likes = max(0, features["avg_comment_likes"])

    userid = random_username(is_bot)
    base_time = 1712150000 + random.randint(0, 864000)

    # Build comment list
    comments = []
    t = base_time
    pool = BOT_COMMENTS_POOL if is_bot else HUMAN_COMMENTS_POOL
    for _ in range(min(num_comments, 10)):
        gap = max(1, int(avg_gap + random.gauss(0, avg_gap * 0.3)))
        t += gap
        comments.append({
            "postid": f"post_{random.randint(10, 99999)}",
            "text": random.choice(pool),
            "timestamp": t,
            "likes": max(0, int(avg_likes + random.gauss(0, max(1, avg_likes * 0.5))))
        })

    # Build like list
    likes = []
    t_like = base_time
    for _ in range(min(num_likes, 10)):
        gap = max(1, int(avg_like_gap_val + random.gauss(0, avg_like_gap_val * 0.3)))
        t_like += gap
        likes.append({
            "postid": f"post_{random.randint(10, 99999)}",
            "timestamp": t_like
        })

    return {
        "userid(username)": userid,
        "List of Comment-level data": json.dumps(comments),
        "List of Like-level data": json.dumps(likes),
        "Is a bot?(bool)": is_bot
    }


def main():
    os.makedirs('data', exist_ok=True)

    # Step 1: Extract numerical features from seed
    df_features = extract_features_from_seed()
    print(f"Feature matrix shape: {df_features.shape}")

    # Step 2: Train CTGAN on features
    print("Training CTGAN on extracted features...")
    discrete_columns = ['is_bot']
    ctgan = CTGAN(epochs=15)
    ctgan.fit(df_features, discrete_columns)

    # Step 3 & 4: Generate synthetic feature rows in chunks to prevent OOM
    target_rows = 10000000
    chunk_size = 50000
    csv_path = 'data/behavioral_amplified_v2.csv'

    print(f"Generating {target_rows} synthetic feature rows in chunks of {chunk_size}...")
    
    # Write header first
    header_written = False
    
    for chunk_idx in range(0, target_rows, chunk_size):
        current_chunk_size = min(chunk_size, target_rows - chunk_idx)
        print(f"Generating chunk {chunk_idx // chunk_size + 1} ({current_chunk_size} rows)...")
        
        synthetic_features = ctgan.sample(current_chunk_size)
        
        output_rows = []
        for _, row in synthetic_features.iterrows():
            output_rows.append(reconstruct_row(row.to_dict()))
            
        df_chunk = pd.DataFrame(output_rows)
        
        mode = 'w' if not header_written else 'a'
        header = not header_written
        
        try:
            df_chunk.to_csv(csv_path, mode=mode, header=header, index=False, encoding='utf-8')
        except Exception as e:
            print(f"Error saving CSV chunk: {e}")
            df_chunk.to_csv(csv_path, mode=mode, header=header, index=False)
            
        header_written = True
        print(f"Progress: {min(chunk_idx + current_chunk_size, target_rows)} / {target_rows} rows saved.")

    print(f"Successfully saved all {target_rows} rows to {csv_path}")

    print(f"Columns: {list(df_chunk.columns)}")
    print(f"\nSample rows from last chunk:")
    print(df_chunk.head(3).to_string())


if __name__ == "__main__":
    main()
