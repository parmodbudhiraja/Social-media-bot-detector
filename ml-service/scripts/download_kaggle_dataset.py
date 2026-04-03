import os
import pandas as pd
import kagglehub

def download_dataset():
    os.makedirs('data', exist_ok=True)
    print("Attempting to download via kagglehub...")
    # Provided by User
    path = kagglehub.dataset_download("rajumavinmar/fake-instagram-profile-dataset")
    print("Path to downloaded files:", path)
    
    # Locate the CSV inside the returned path
    csv_files = [f for f in os.listdir(path) if f.endswith('.csv')]
    if not csv_files:
        raise Exception("No CSV file found in kagglehub download.")
    
    source_file = os.path.join(path, csv_files[0])
    df = pd.read_csv(source_file)
    print(f"Dataset loaded with shape: {df.shape}")
    
    # Map raw kaggle columns to our standardized internal format
    rename_map = {
        'profile pic': 'profile_pic',
        'nums/length username': 'nums/length_username',
        'fullname words': 'fullname_words',
        'nums/length fullname': 'nums/length_fullname',
        'name==username': 'name==username',
        'description length': 'description_length',
        'external URL': 'external_URL',
        'private': 'private',
        '#posts': 'posts',
        '#followers': 'followers',
        '#follows': 'following',
        'fake': 'is_fake'
    }
    df = df.rename(columns=rename_map)
    print(f"Standardized Columns: {list(df.columns)}")
    
    # Write to local data folder for standard naming
    df.to_csv('data/kaggle_instagram_profiles.csv', index=False)
    print("Successfully downloaded and saved Kaggle dataset to data/kaggle_instagram_profiles.csv")

if __name__ == "__main__":
    download_dataset()
