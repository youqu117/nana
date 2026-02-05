import os
import shutil
import json
import re

# Paths
ROOT_DIR = r"E:\Uself_document\T_document\nana"
V01_DIR = os.path.join(ROOT_DIR, "pixel_pet_assets_v01", "assets", "pets")
V02_DIR = os.path.join(ROOT_DIR, "pixel_pet_assets_v02", "new_assets")
TARGET_DIR = os.path.join(ROOT_DIR, "app", "src", "main", "assets", "pets")

# Ensure target directory exists
if not os.path.exists(TARGET_DIR):
    os.makedirs(TARGET_DIR)

def create_manifest(pet_id, name):
    return {
        "id": pet_id,
        "name": name,
        "version": 1,
        "preview": f"pets/{pet_id}/preview.png",
        "static_normal": f"pets/{pet_id}/static/pet_normal.png",
        "static_tongue": f"pets/{pet_id}/static/pet_normal.png", # Fallback
        "idle_sheet": f"pets/{pet_id}/anim/idle/sheet.png",
        "idle_anim": f"pets/{pet_id}/anim/idle/anim.json",
        "default_scale": 3,
        "hitbox": { "x": 8, "y": 8, "w": 32, "h": 32 },
        "anchors": {
            "root_x": 24, "root_y": 40,
            "head_x": 24, "head_y": 16,
            "face_x": 24, "face_y": 22
        }
    }

def create_anim_json():
    return {
        "name": "idle",
        "loop": True,
        "frames": [
            { "x": 0, "y": 0, "w": 48, "h": 48, "duration": 200 },
            { "x": 48, "y": 0, "w": 48, "h": 48, "duration": 200 },
            { "x": 96, "y": 0, "w": 48, "h": 48, "duration": 200 },
            { "x": 144, "y": 0, "w": 48, "h": 48, "duration": 200 }
        ]
    }

# 1. Process V01 (Already structured, just copy)
print("Processing V01 assets...")
if os.path.exists(V01_DIR):
    for item in os.listdir(V01_DIR):
        src_path = os.path.join(V01_DIR, item)
        dst_path = os.path.join(TARGET_DIR, item)
        if os.path.isdir(src_path):
            if os.path.exists(dst_path):
                shutil.rmtree(dst_path)
            shutil.copytree(src_path, dst_path)
            print(f"Copied V01 pet: {item}")

# 2. Process V02 (Flat files)
print("Processing V02 assets...")
if os.path.exists(V02_DIR):
    # Group files by pet name
    # Expected format: {name}_static.png, {name}_idle_sheet.png
    files = os.listdir(V02_DIR)
    pets = {}
    
    for f in files:
        if f.endswith("_static.png"):
            name = f.replace("_static.png", "")
            if name not in pets: pets[name] = {}
            pets[name]["static"] = os.path.join(V02_DIR, f)
        elif f.endswith("_idle_sheet.png"):
            name = f.replace("_idle_sheet.png", "")
            if name not in pets: pets[name] = {}
            pets[name]["sheet"] = os.path.join(V02_DIR, f)

    for pet_id, paths in pets.items():
        if "static" in paths and "sheet" in paths:
            print(f"Processing V02 pet: {pet_id}")
            
            pet_dir = os.path.join(TARGET_DIR, pet_id)
            if not os.path.exists(pet_dir):
                os.makedirs(pet_dir)
                os.makedirs(os.path.join(pet_dir, "static"))
                os.makedirs(os.path.join(pet_dir, "anim", "idle"))

            # Copy images
            shutil.copy2(paths["static"], os.path.join(pet_dir, "static", "pet_normal.png"))
            shutil.copy2(paths["sheet"], os.path.join(pet_dir, "anim", "idle", "sheet.png"))
            
            # Create Preview (Reuse static)
            shutil.copy2(paths["static"], os.path.join(pet_dir, "preview.png"))

            # Create JSONs
            manifest = create_manifest(pet_id, pet_id.replace("_", " ").title())
            with open(os.path.join(pet_dir, "manifest.json"), "w") as f:
                json.dump(manifest, f, indent=2)
            
            anim = create_anim_json()
            with open(os.path.join(pet_dir, "anim", "idle", "anim.json"), "w") as f:
                json.dump(anim, f, indent=2)

print("Migration complete.")
