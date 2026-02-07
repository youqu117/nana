import os
import json
import shutil

ROOT = r"E:\Uself_document\T_document\nana\app\src\main\assets"
BATCH_DIR = os.path.join(ROOT, "batches", "batch4")
PETS_DIR = os.path.join(ROOT, "pets")

def process_batch():
    manifest_path = os.path.join(BATCH_DIR, "manifest.json")
    if not os.path.exists(manifest_path):
        print("No manifest found in batch4")
        return

    with open(manifest_path, 'r') as f:
        items = json.load(f)

    # Group by animal_color
    pets = {}
    for item in items:
        key = f"{item['animal']}"
        if item.get('color') and item['color'] != 'default':
            key += f"_{item['color']}"
        
        if key not in pets:
            pets[key] = {}
        
        if item['type'] == 'static':
            pets[key]['static'] = item['file']
        elif item['type'] == 'idle_sheet':
            pets[key]['sheet'] = item['file']

    for pet_id, assets in pets.items():
        print(f"Processing {pet_id}...")
        
        target_dir = os.path.join(PETS_DIR, pet_id)
        os.makedirs(target_dir, exist_ok=True)
        
        # Paths
        src_static = os.path.join(BATCH_DIR, assets.get('static', assets.get('sheet')))
        
        # Copy Images
        shutil.copy2(src_static, os.path.join(target_dir, "preview.png"))
        
        # Create Manifest
        # FIX: Use snake_case keys to match AssetLoader.kt expectations
        # FIX: Use default_scale: 1 for high-res assets
        manifest = {
            "id": pet_id,
            "name": pet_id.replace("_", " ").title(),
            "version": 1,
            "preview": f"pets/{pet_id}/preview.png",
            "static_normal": f"pets/{pet_id}/preview.png",
            "static_tongue": f"pets/{pet_id}/preview.png",
            "idle_sheet": f"pets/{pet_id}/preview.png",
            "idle_anim": f"pets/{pet_id}/anim/idle/anim.json",
            "default_scale": 1,
            "hitbox": { "x": 0, "y": 0, "w": 48, "h": 48 },
            "anchors": {
                "root_x": 24, "root_y": 48,
                "head_x": 24, "head_y": 10,
                "face_x": 24, "face_y": 15
            }
        }
        
        with open(os.path.join(target_dir, "manifest.json"), 'w') as f:
            json.dump(manifest, f, indent=2)
            
        # Create Anim
        anim_dir = os.path.join(target_dir, "anim", "idle")
        os.makedirs(anim_dir, exist_ok=True)
        
        anim = {
            "name": "idle",
            "loop": True,
            "frames": [
                { "x": 0, "y": 0, "w": 48, "h": 48, "duration": 1000 } # Single frame
            ]
        }
        
        with open(os.path.join(anim_dir, "anim.json"), 'w') as f:
            json.dump(anim, f, indent=2)

if __name__ == "__main__":
    process_batch()
