import pandas as pd
import numpy as np
import os
from ctgan import CTGAN

def create_seed_dataset():
    """Generate a programmatic diverse seed dataset simulating LLM generated data."""
    print("Generating diverse seed dataset...")
    np.random.seed(42)
    n_seed = 500
    
    # Fake users have weird ratios
    fake_followers = np.random.randint(0, 150, size=n_seed)
    fake_following = np.random.randint(1000, 7500, size=n_seed)
    fake_posts = np.random.randint(0, 10, size=n_seed)
    fake_engagement = np.random.uniform(0.01, 0.05, size=n_seed)
    fake_rep_ratio = np.random.uniform(0.7, 1.0, size=n_seed) # Highly repetitive comments
    fake_label = np.ones(n_seed)
    
    # Real users have healthy ratios
    real_followers = np.random.randint(100, 5000, size=n_seed)
    real_following = np.random.randint(100, 1000, size=n_seed)
    real_posts = np.random.randint(10, 500, size=n_seed)
    real_engagement = np.random.uniform(0.05, 0.25, size=n_seed)
    real_rep_ratio = np.random.uniform(0.0, 0.3, size=n_seed) # Non-repetitive comments
    real_label = np.zeros(n_seed)
    
    df_fake = pd.DataFrame({
        'followers': fake_followers, 'following': fake_following,
        'posts': fake_posts, 'engagement': fake_engagement,
        'repetitive_ratio': fake_rep_ratio, 'is_fake': fake_label
    })
    
    df_real = pd.DataFrame({
        'followers': real_followers, 'following': real_following,
        'posts': real_posts, 'engagement': real_engagement,
        'repetitive_ratio': real_rep_ratio, 'is_fake': real_label
    })
    
    df = pd.concat([df_fake, df_real]).sample(frac=1).reset_index(drop=True)
    return df

def generate_synthetic_data(seed_df, target_rows=100000):
    print(f"Training CTGAN on seed dataset of {len(seed_df)} rows...")
    discrete_columns = ['is_fake']
    
    ctgan = CTGAN(epochs=10) # 10 epochs for faster hackathon training
    ctgan.fit(seed_df, discrete_columns)
    
    print(f"Generating {target_rows} synthetic rows...")
    synthetic_data = ctgan.sample(target_rows)
    
    os.makedirs('data', exist_ok=True)
    csv_path = 'data/synthetic_dataset.csv'
    synthetic_data.to_csv(csv_path, index=False)
    print(f"Saved synthetic dataset to {csv_path}")
    return synthetic_data

if __name__ == "__main__":
    seed_data = create_seed_dataset()
    # Execute generation targeting 100000 rows for full scale
    synthetic_df = generate_synthetic_data(seed_data, target_rows=100000)
    
    print("Dataset pipeline complete.")
