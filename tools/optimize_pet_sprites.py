#!/usr/bin/env python3
import os
import json
import shutil
from PIL import Image

PETS_DIR = r"E:\Cloud-Github\nana\app\src\main\assets\pets"
ARTIFACT_DIR = r"C:\Users\youqu\.gemini\antigravity\brain\a7fcbbde-e2c9-4a83-a19c-3f17da7a7c1c"

def remove_white_background(img: Image.Image, threshold: int = 238) -> Image.Image:
    """把纯白或接近纯白的底色转换为完全透明 (RGBA)."""
    img = img.convert("RGBA")
    datas = img.getdata()
    new_data = []
    for item in datas:
        r, g, b, a = item
        if r >= threshold and g >= threshold and b >= threshold:
            new_data.append((0, 0, 0, 0))
        else:
            new_data.append((r, g, b, a))
    img.putdata(new_data)
    return img

def create_idle_sprite_sheet(base_img: Image.Image) -> tuple[Image.Image, list]:
    """根据基础精灵图，合成包含 4 帧平呼吸与微动的 Sprite Sheet，并生成 anim.json 帧数据."""
    w, h = base_img.size
    
    # 建立 4 帧的横向 Sprite Sheet (宽度 = 4 * w, 高度 = h)
    sheet = Image.new("RGBA", (w * 4, h), (0, 0, 0, 0))
    
    # 帧 0：原始静止帧
    sheet.paste(base_img, (0, 0))
    
    # 帧 1：平移 1px 模拟呼吸起伏
    frame1 = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    frame1.paste(base_img, (0, -1))
    sheet.paste(frame1, (w, 0))
    
    # 帧 2：平移 2px 模拟最高姿态
    frame2 = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    frame2.paste(base_img, (0, -2))
    sheet.paste(frame2, (w * 2, 0))
    
    # 帧 3：平移 1px 回落
    frame3 = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    frame3.paste(base_img, (0, -1))
    sheet.paste(frame3, (w * 3, 0))
    
    frames_json = [
        {"x": 0, "y": 0, "w": w, "h": h, "duration": 280},
        {"x": w, "y": 0, "w": w, "h": h, "duration": 220},
        {"x": w * 2, "y": 0, "w": w, "h": h, "duration": 280},
        {"x": w * 3, "y": 0, "w": w, "h": h, "duration": 220}
    ]
    return sheet, frames_json

def process_all_pets():
    os.makedirs(ARTIFACT_DIR, exist_ok=True)
    
    for pet_id in os.listdir(PETS_DIR):
        pet_dir = os.path.join(PETS_DIR, pet_id)
        if not os.path.isdir(pet_dir):
            continue
            
        manifest_path = os.path.join(pet_dir, "manifest.json")
        if not os.path.exists(manifest_path):
            continue
            
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
            
        preview_file = os.path.join(pet_dir, "preview.png")
        if not os.path.exists(preview_file):
            continue
            
        print(f"Processing pet asset: {pet_id}...")
        
        # 1. 打开并去白底，保存透明底预览图
        img = Image.open(preview_file)
        clean_img = remove_white_background(img)
        clean_img.save(preview_file, "PNG")
        
        # 复制透明预览图到 artifact 目录
        artifact_preview = os.path.join(ARTIFACT_DIR, f"pet_{pet_id}.png")
        clean_img.save(artifact_preview, "PNG")
        
        # 2. 生成多帧动画 Sprite Sheet (idle_sheet.png)
        sheet_img, frames_data = create_idle_sprite_sheet(clean_img)
        sheet_path = os.path.join(pet_dir, "idle_sheet.png")
        sheet_img.save(sheet_path, "PNG")
        
        # 3. 更新 anim/idle/anim.json
        anim_dir = os.path.join(pet_dir, "anim", "idle")
        os.makedirs(anim_dir, exist_ok=True)
        anim_json_path = os.path.join(anim_dir, "anim.json")
        
        anim_data = {
            "name": "idle",
            "loop": True,
            "frames": frames_data
        }
        with open(anim_json_path, "w", encoding="utf-8") as f:
            json.dump(anim_data, f, indent=2)
            
        # 4. 更新 manifest.json 中的路径和配置
        manifest["preview"] = f"pets/{pet_id}/preview.png"
        manifest["static_normal"] = f"pets/{pet_id}/preview.png"
        manifest["static_tongue"] = f"pets/{pet_id}/preview.png"
        manifest["idle_sheet"] = f"pets/{pet_id}/idle_sheet.png"
        manifest["idle_anim"] = f"pets/{pet_id}/anim/idle/anim.json"
        manifest["hitbox"] = {
            "x": 0,
            "y": 0,
            "w": clean_img.width,
            "h": clean_img.height
        }
        manifest["anchors"] = {
            "root_x": clean_img.width // 2,
            "root_y": clean_img.height,
            "head_x": clean_img.width // 2,
            "head_y": clean_img.height // 4,
            "face_x": clean_img.width // 2,
            "face_y": clean_img.height // 3
        }
        
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2, ensure_ascii=False)
            
        print(f"Successfully optimized {pet_id}: transparent PNG + 4-frame idle animation generated.")

if __name__ == "__main__":
    process_all_pets()
