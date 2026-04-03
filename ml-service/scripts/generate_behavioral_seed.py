"""
Generates a behavioral seed dataset programmatically with realistic bot vs human patterns.
This mirrors what Gemini would produce but runs instantly without API rate limits.
The patterns are based on well-documented Instagram bot behavior research.
"""
import os
import json
import random
import string
import time as time_mod

# Bot comment templates (repetitive, generic, spammy)
BOT_COMMENTS = [
    "Nice pic! 🔥", "Check my profile!", "DM me for collab 💯", "Amazing! ❤️",
    "Follow me back!", "Love this! 😍", "Great content! 👏", "Wow! 🙌",
    "Beautiful! ✨", "DM for promotions!", "Nice! 🔥🔥🔥", "Check bio for link!",
    "Cool! 😎", "Follow for follow! 🤝", "Love it! ❤️❤️", "Great post! 💯",
    "Awesome! 🙏", "Like for like!", "So good! 👌", "Check out my page!",
    "🔥🔥🔥", "❤️❤️❤️", "👏👏", "💯💯", "😍😍😍",
    "DM me!", "Nice content!", "Follow back plz!", "👀", "🙌🙌🙌",
]

# Human comment templates (varied, contextual, meaningful)
HUMAN_COMMENTS = [
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
    "I've been meaning to visit this museum, your photos convinced me",
    "Such a thoughtful caption, really resonated with me",
    "Happy birthday! Hope you had an amazing celebration 🎂",
    "This view is breathtaking, nature never ceases to amaze",
    "Just ordered the same book after seeing your review, can't wait to read it",
    "Your kids are growing up so fast! Adorable family photo",
    "The architecture in this building is fascinating, love the geometric patterns",
    "Finally someone who understands the struggle of morning workouts haha",
    "This smoothie bowl looks delicious, I need to try this combination",
    "Your garden is goals! Any tips for growing tomatoes?",
    "I remember when we used to hang out at this spot, good times",
    "Love seeing your creative process, the behind-the-scenes content is great",
    "This hiking trail looks challenging but worth it for that view!",
]

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
            f"{random.choice(first_names)}_{random.choice(['photo', 'travel', 'art', 'life', 'adventures'])}",
            f"{random.choice(first_names)}.{random.choice(first_names)[:3]}",
            f"the.real.{random.choice(first_names)}",
            f"{random.choice(first_names)}_{random.randint(90, 99)}",
        ]
    return random.choice(patterns)

def generate_user(is_bot: bool) -> dict:
    userid = random_username(is_bot)
    base_time = 1700000000  # Nov 2023
    
    num_comments = random.randint(5, 10)
    num_likes = random.randint(5, 10)
    
    comments = []
    likes = []
    
    if is_bot:
        # Bot behavior: rapid-fire comments, short time gaps (seconds to minutes)
        t = base_time + random.randint(0, 86400)  # random start within a day
        for _ in range(num_comments):
            gap = random.randint(1, 120)  # 1-120 seconds between comments
            t += gap
            comments.append({
                "postid": f"post_{random.randint(10000, 99999)}",
                "text": random.choice(BOT_COMMENTS),
                "timestamp": t,
                "likes_on_comment": random.randint(0, 2)  # bots get few likes
            })
        
        # Bot likes: mass-liking in bursts
        t_like = base_time + random.randint(0, 86400)
        for _ in range(num_likes):
            t_like += random.randint(1, 30)  # 1-30 seconds between likes
            likes.append({
                "postid": f"post_{random.randint(10000, 99999)}",
                "timestamp": t_like
            })
    else:
        # Human behavior: spread over days/weeks, varied content
        t = base_time
        for _ in range(num_comments):
            gap = random.randint(3600, 604800)  # 1 hour to 7 days between comments
            t += gap
            comments.append({
                "postid": f"post_{random.randint(10000, 99999)}",
                "text": random.choice(HUMAN_COMMENTS),
                "timestamp": t,
                "likes_on_comment": random.randint(1, 50)  # humans get more likes
            })
        
        # Human likes: organic, spread over time
        t_like = base_time
        for _ in range(num_likes):
            t_like += random.randint(1800, 259200)  # 30 min to 3 days between likes
            likes.append({
                "postid": f"post_{random.randint(10000, 99999)}",
                "timestamp": t_like
            })
    
    return {
        "userid": userid,
        "comments": comments,
        "likes": likes,
        "is_bot": is_bot
    }

def main():
    os.makedirs('data', exist_ok=True)
    
    target_per_class = 500  # 500 bots + 500 real = 1000 seed users
    
    print(f"Generating {target_per_class} bot users...")
    bots = [generate_user(is_bot=True) for _ in range(target_per_class)]
    
    print(f"Generating {target_per_class} real users...")
    reals = [generate_user(is_bot=False) for _ in range(target_per_class)]
    
    all_users = bots + reals
    random.shuffle(all_users)
    
    with open('data/behavioral_seed.json', 'w') as f:
        json.dump(all_users, f, indent=2)
    
    print(f"Generated {len(all_users)} behavior profiles ({target_per_class} bots, {target_per_class} real)")
    print("Saved to data/behavioral_seed.json")

if __name__ == "__main__":
    main()
