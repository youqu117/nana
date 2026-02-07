import os
import json

base_path = r"e:\Uself_document\T_document\nana\app\src\main\assets"
pets_path = os.path.join(base_path, "pets")

def check_file(path):
    # Check regular
    full_path = os.path.join(base_path, path)
    if os.path.exists(full_path):
        return True
    # Check base64
    if os.path.exists(full_path + ".base64"):
        return True
    return False

print(f"Scanning {pets_path}...")
for pet_id in os.listdir(pets_path):
    pet_dir = os.path.join(pets_path, pet_id)
    if not os.path.isdir(pet_dir):
        continue
        
    manifest_path = os.path.join(pet_dir, "manifest.json")
    if not os.path.exists(manifest_path):
        print(f"[ERROR] {pet_id}: No manifest.json")
        continue
        
    try:
        with open(manifest_path, 'r') as f:
            data = json.load(f)
            
        required_fields = ["preview", "static_normal"]
        for field in required_fields:
            if field not in data or not data[field]:
                print(f"[ERROR] {pet_id}: Missing field '{field}'")
            else:
                path = data[field]
                # Fix: manifest paths are relative to assets root, not pets dir
                # But some are absolute relative to assets
                if not check_file(path):
                     print(f"[ERROR] {pet_id}: File not found for '{field}': {path}")
                else:
                    print(f"[OK] {pet_id}: {field} -> {path}")

    except Exception as e:
        print(f"[ERROR] {pet_id}: Invalid JSON - {e}")
