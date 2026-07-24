package com.pixelpet.util

import android.graphics.*

object PixelPetGenerator {

    data class PixelColor(val r: Int, val g: Int, val b: Int, val a: Int = 255) {
        fun toInt(): Int = android.graphics.Color.argb(a, r, g, b)
    }

    fun generatePetBitmap(petId: String, width: Int = 48, height: Int = 48): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        when (petId) {
            "cat_orange" -> drawCatOrange(canvas, paint, width, height)
            "dog_shiba" -> drawDogShiba(canvas, paint, width, height)
            "dragon" -> drawDragon(canvas, paint, width, height)
            "panda" -> drawPanda(canvas, paint, width, height)
            "penguin_chick" -> drawPenguinChick(canvas, paint, width, height)
            "turtle" -> drawTurtle(canvas, paint, width, height)
            "spider" -> drawSpider(canvas, paint, width, height)
            "huan_huan" -> drawHuanHuan(canvas, paint, width, height)
            "la_la" -> drawLaLa(canvas, paint, width, height)
            "qi_qi" -> drawQiQi(canvas, paint, width, height)
            "you_ha" -> drawYouHa(canvas, paint, width, height)
            else -> drawDefaultPet(canvas, paint, width, height)
        }

        return bitmap
    }

    fun generatePetSpriteSheet(petId: String, frameWidth: Int = 48, frameHeight: Int = 48, frameCount: Int = 4): Bitmap {
        val bitmap = Bitmap.createBitmap(frameWidth * frameCount, frameHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        for (i in 0 until frameCount) {
            val x = i * frameWidth
            when (petId) {
                "cat_orange" -> drawCatOrangeFrame(canvas, paint, x, 0, frameWidth, frameHeight, i)
                "dog_shiba" -> drawDogShibaFrame(canvas, paint, x, 0, frameWidth, frameHeight, i)
                "dragon" -> drawDragonFrame(canvas, paint, x, 0, frameWidth, frameHeight, i)
                "panda" -> drawPandaFrame(canvas, paint, x, 0, frameWidth, frameHeight, i)
                "penguin_chick" -> drawPenguinChickFrame(canvas, paint, x, 0, frameWidth, frameHeight, i)
                "turtle" -> drawTurtleFrame(canvas, paint, x, 0, frameWidth, frameHeight, i)
                "spider" -> drawSpiderFrame(canvas, paint, x, 0, frameWidth, frameHeight, i)
                else -> drawDefaultPet(canvas, paint, frameWidth, frameHeight)
            }
        }

        return bitmap
    }

    private fun drawCatOrange(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(255, 165, 0).toInt()
        canvas.drawRect(c - 8, midY - 12, c + 8, midY + 8, paint)
        canvas.drawRect(c - 12, midY - 4, c + 12, midY + 8, paint)

        paint.color = PixelColor(200, 100, 0).toInt()
        canvas.drawRect(c - 6, midY + 2, c + 6, midY + 8, paint)

        paint.color = PixelColor(255, 200, 150).toInt()
        canvas.drawRect(c - 4, midY - 2, c + 4, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 5, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 165, 0).toInt()
        canvas.drawRect(c - 12, midY - 12, c - 10, midY - 4, paint)
        canvas.drawRect(c + 10, midY - 12, c + 12, midY - 4, paint)

        paint.color = PixelColor(200, 100, 0).toInt()
        canvas.drawRect(c - 12, midY - 10, c - 10, midY - 6, paint)
        canvas.drawRect(c + 10, midY - 10, c + 12, midY - 6, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawLine(c - 8, midY - 1, c + 8, midY - 1, paint)
    }

    private fun drawCatOrangeFrame(canvas: Canvas, paint: Paint, x: Int, y: Int, w: Int, h: Int, frame: Int) {
        val c = x + w / 2
        val midY = y + h / 2
        val tailOffset = when (frame) {
            0 -> 0
            1 -> -2
            2 -> 0
            3 -> 2
            else -> 0
        }

        paint.color = PixelColor(255, 165, 0).toInt()
        canvas.drawRect(c - 8, midY - 12, c + 8, midY + 8, paint)
        canvas.drawRect(c - 12, midY - 4, c + 12, midY + 8, paint)

        paint.color = PixelColor(200, 100, 0).toInt()
        canvas.drawRect(c - 6, midY + 2, c + 6, midY + 8, paint)

        paint.color = PixelColor(255, 200, 150).toInt()
        canvas.drawRect(c - 4, midY - 2, c + 4, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 5, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 165, 0).toInt()
        canvas.drawRect(c - 12, midY - 12, c - 10, midY - 4, paint)
        canvas.drawRect(c + 10, midY - 12, c + 12, midY - 4, paint)

        paint.color = PixelColor(200, 100, 0).toInt()
        canvas.drawRect(c - 12, midY - 10, c - 10, midY - 6, paint)
        canvas.drawRect(c + 10, midY - 10, c + 12, midY - 6, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawLine(c - 8, midY - 1, c + 8, midY - 1, paint)

        paint.color = PixelColor(255, 165, 0).toInt()
        canvas.drawRect(c - 16 + tailOffset, midY, c - 14 + tailOffset, midY + 4, paint)
        canvas.drawRect(c - 18 + tailOffset, midY + 4, c - 14 + tailOffset, midY + 6, paint)
    }

    private fun drawDogShiba(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(255, 200, 100).toInt()
        canvas.drawRect(c - 10, midY - 10, c + 10, midY + 10, paint)

        paint.color = PixelColor(200, 150, 80).toInt()
        canvas.drawRect(c - 8, midY + 4, c + 8, midY + 10, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 6, midY - 2, c + 6, midY + 4, paint)

        paint.color = PixelColor(200, 150, 80).toInt()
        canvas.drawRect(c - 10, midY - 10, c - 6, midY - 2, paint)
        canvas.drawRect(c + 6, midY - 10, c + 10, midY - 2, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 6, midY - 6, 2.5f, paint)
        canvas.drawCircle(c + 6, midY - 6, 2.5f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 6, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 6, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 200, 100).toInt()
        canvas.drawRect(c - 2, midY + 2, c + 2, midY + 6, paint)
    }

    private fun drawDogShibaFrame(canvas: Canvas, paint: Paint, x: Int, y: Int, w: Int, h: Int, frame: Int) {
        val c = x + w / 2
        val midY = y + h / 2
        val earWag = if (frame % 2 == 0) 0 else 1

        paint.color = PixelColor(255, 200, 100).toInt()
        canvas.drawRect(c - 10, midY - 10, c + 10, midY + 10, paint)

        paint.color = PixelColor(200, 150, 80).toInt()
        canvas.drawRect(c - 8, midY + 4, c + 8, midY + 10, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 6, midY - 2, c + 6, midY + 4, paint)

        paint.color = PixelColor(200, 150, 80).toInt()
        canvas.drawRect(c - 10, midY - 10 - earWag, c - 6, midY - 2, paint)
        canvas.drawRect(c + 6, midY - 10 + earWag, c + 10, midY - 2, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 6, midY - 6, 2.5f, paint)
        canvas.drawCircle(c + 6, midY - 6, 2.5f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 6, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 6, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 200, 100).toInt()
        canvas.drawRect(c - 2, midY + 2, c + 2, midY + 6, paint)
    }

    private fun drawDragon(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 12, midY - 10, c + 16, midY + 8, paint)

        paint.color = PixelColor(150, 70, 30).toInt()
        canvas.drawRect(c - 10, midY, c + 14, midY + 8, paint)

        paint.color = PixelColor(255, 200, 50).toInt()
        canvas.drawRect(c + 8, midY - 6, c + 18, midY + 2, paint)

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 16, midY - 4, c - 12, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 3, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 3, midY - 6, 1.5f, paint)

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 14, midY - 14, c - 10, midY - 8, paint)
        canvas.drawRect(c - 6, midY - 14, c - 2, midY - 8, paint)

        paint.color = PixelColor(255, 200, 50).toInt()
        canvas.drawRect(c - 14, midY - 12, c - 10, midY - 10, paint)
        canvas.drawRect(c - 6, midY - 12, c - 2, midY - 10, paint)

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 20, midY + 2, c - 16, midY + 8, paint)
        canvas.drawRect(c - 18, midY + 8, c - 14, midY + 10, paint)
    }

    private fun drawDragonFrame(canvas: Canvas, paint: Paint, x: Int, y: Int, w: Int, h: Int, frame: Int) {
        val c = x + w / 2
        val midY = y + h / 2
        val wingFlap = if (frame % 2 == 0) 0 else -2

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 12, midY - 10, c + 16, midY + 8, paint)

        paint.color = PixelColor(150, 70, 30).toInt()
        canvas.drawRect(c - 10, midY, c + 14, midY + 8, paint)

        paint.color = PixelColor(255, 200, 50).toInt()
        canvas.drawRect(c + 8, midY - 6, c + 18, midY + 2, paint)

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 16, midY - 4, c - 12, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 3, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 3, midY - 6, 1.5f, paint)

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 14, midY - 14 + wingFlap, c - 10, midY - 8 + wingFlap, paint)
        canvas.drawRect(c - 6, midY - 14 + wingFlap, c - 2, midY - 8 + wingFlap, paint)

        paint.color = PixelColor(255, 200, 50).toInt()
        canvas.drawRect(c - 14, midY - 12 + wingFlap, c - 10, midY - 10 + wingFlap, paint)
        canvas.drawRect(c - 6, midY - 12 + wingFlap, c - 2, midY - 10 + wingFlap, paint)

        paint.color = PixelColor(200, 100, 50).toInt()
        canvas.drawRect(c - 20, midY + 2, c - 16, midY + 8, paint)
        canvas.drawRect(c - 18, midY + 8, c - 14, midY + 10, paint)
    }

    private fun drawPanda(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 10, midY - 12, c + 10, midY + 10, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawRect(c - 10, midY - 12, c - 4, midY - 4, paint)
        canvas.drawRect(c + 4, midY - 12, c + 10, midY - 4, paint)
        canvas.drawRect(c - 8, midY + 4, c + 8, midY + 10, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 6, midY - 6, 3f, paint)
        canvas.drawCircle(c + 6, midY - 6, 3f, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawCircle(c - 6, midY - 6, 2f, paint)
        canvas.drawCircle(c + 6, midY - 6, 2f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 6, midY - 6, 1f, paint)
        canvas.drawCircle(c + 6, midY - 6, 1f, paint)

        paint.color = PixelColor(255, 180, 180).toInt()
        canvas.drawRect(c - 2, midY + 2, c + 2, midY + 6, paint)
    }

    private fun drawPandaFrame(canvas: Canvas, paint: Paint, x: Int, y: Int, w: Int, h: Int, frame: Int) {
        val c = x + w / 2
        val midY = y + h / 2
        val bounce = when (frame) {
            0 -> 0
            1 -> -1
            2 -> 0
            3 -> 1
            else -> 0
        }

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 10, midY - 12 + bounce, c + 10, midY + 10 + bounce, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawRect(c - 10, midY - 12 + bounce, c - 4, midY - 4 + bounce, paint)
        canvas.drawRect(c + 4, midY - 12 + bounce, c + 10, midY - 4 + bounce, paint)
        canvas.drawRect(c - 8, midY + 4 + bounce, c + 8, midY + 10 + bounce, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 6, midY - 6 + bounce, 3f, paint)
        canvas.drawCircle(c + 6, midY - 6 + bounce, 3f, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawCircle(c - 6, midY - 6 + bounce, 2f, paint)
        canvas.drawCircle(c + 6, midY - 6 + bounce, 2f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 6, midY - 6 + bounce, 1f, paint)
        canvas.drawCircle(c + 6, midY - 6 + bounce, 1f, paint)

        paint.color = PixelColor(255, 180, 180).toInt()
        canvas.drawRect(c - 2, midY + 2 + bounce, c + 2, midY + 6 + bounce, paint)
    }

    private fun drawPenguinChick(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 8, midY - 10, c + 8, midY + 10, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawRect(c - 6, midY + 2, c + 6, midY + 10, paint)

        paint.color = PixelColor(255, 200, 150).toInt()
        canvas.drawCircle(c, midY - 2, 4f, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 2.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 2.5f, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 100, 100).toInt()
        canvas.drawRect(c - 3, midY + 2, c + 3, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 10, midY + 6, c - 8, midY + 10, paint)
        canvas.drawRect(c + 8, midY + 6, c + 10, midY + 10, paint)
    }

    private fun drawPenguinChickFrame(canvas: Canvas, paint: Paint, x: Int, y: Int, w: Int, h: Int, frame: Int) {
        val c = x + w / 2
        val midY = y + h / 2
        val wobble = when (frame) {
            0 -> 0
            1 -> -1
            2 -> 0
            3 -> 1
            else -> 0
        }

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 8 + wobble, midY - 10, c + 8 + wobble, midY + 10, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawRect(c - 6 + wobble, midY + 2, c + 6 + wobble, midY + 10, paint)

        paint.color = PixelColor(255, 200, 150).toInt()
        canvas.drawCircle(c + wobble, midY - 2, 4f, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5 + wobble, midY - 6, 2.5f, paint)
        canvas.drawCircle(c + 5 + wobble, midY - 6, 2.5f, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawCircle(c - 5 + wobble, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5 + wobble, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 100, 100).toInt()
        canvas.drawRect(c - 3 + wobble, midY + 2, c + 3 + wobble, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 10 + wobble, midY + 6, c - 8 + wobble, midY + 10, paint)
        canvas.drawRect(c + 8 + wobble, midY + 6, c + 10 + wobble, midY + 10, paint)
    }

    private fun drawTurtle(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(100, 180, 100).toInt()
        canvas.drawRect(c - 12, midY - 8, c + 12, midY + 8, paint)

        paint.color = PixelColor(80, 150, 80).toInt()
        canvas.drawRect(c - 8, midY - 4, c + 8, midY + 4, paint)

        paint.color = PixelColor(60, 120, 60).toInt()
        canvas.drawLine(c, midY - 8, c, midY + 8, paint)
        canvas.drawLine(c - 8, midY, c + 8, midY, paint)
        canvas.drawLine(c - 4, midY - 4, c + 4, midY - 4, paint)
        canvas.drawLine(c - 4, midY + 4, c + 4, midY + 4, paint)

        paint.color = PixelColor(150, 120, 80).toInt()
        canvas.drawRect(c + 12, midY - 4, c + 18, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c + 14, midY - 2, 1.5f, paint)
        canvas.drawCircle(c + 16, midY - 2, 1.5f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c + 14, midY - 2, 0.8f, paint)
        canvas.drawCircle(c + 16, midY - 2, 0.8f, paint)

        paint.color = PixelColor(150, 120, 80).toInt()
        canvas.drawRect(c - 18, midY - 2, c - 12, midY, paint)
        canvas.drawRect(c - 18, midY, c - 12, midY + 2, paint)
        canvas.drawRect(c - 18, midY + 2, c - 14, midY + 4, paint)
        canvas.drawRect(c - 18, midY + 4, c - 14, midY + 6, paint)
    }

    private fun drawTurtleFrame(canvas: Canvas, paint: Paint, x: Int, y: Int, w: Int, h: Int, frame: Int) {
        val c = x + w / 2
        val midY = y + h / 2
        val swim = if (frame % 2 == 0) 0 else 1

        paint.color = PixelColor(100, 180, 100).toInt()
        canvas.drawRect(c - 12, midY - 8 + swim, c + 12, midY + 8 + swim, paint)

        paint.color = PixelColor(80, 150, 80).toInt()
        canvas.drawRect(c - 8, midY - 4 + swim, c + 8, midY + 4 + swim, paint)

        paint.color = PixelColor(60, 120, 60).toInt()
        canvas.drawLine(c, midY - 8 + swim, c, midY + 8 + swim, paint)
        canvas.drawLine(c - 8, midY + swim, c + 8, midY + swim, paint)
        canvas.drawLine(c - 4, midY - 4 + swim, c + 4, midY - 4 + swim, paint)
        canvas.drawLine(c - 4, midY + 4 + swim, c + 4, midY + 4 + swim, paint)

        paint.color = PixelColor(150, 120, 80).toInt()
        canvas.drawRect(c + 12, midY - 4, c + 18, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c + 14, midY - 2, 1.5f, paint)
        canvas.drawCircle(c + 16, midY - 2, 1.5f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c + 14, midY - 2, 0.8f, paint)
        canvas.drawCircle(c + 16, midY - 2, 0.8f, paint)

        paint.color = PixelColor(150, 120, 80).toInt()
        val flipperOffset = if (frame % 2 == 0) 0 else -2
        canvas.drawRect(c - 18, midY - 2 + flipperOffset, c - 12, midY + flipperOffset, paint)
        canvas.drawRect(c - 18, midY + flipperOffset, c - 12, midY + 2 + flipperOffset, paint)
        canvas.drawRect(c - 18, midY + 2 + flipperOffset, c - 14, midY + 4 + flipperOffset, paint)
        canvas.drawRect(c - 18, midY + 4 + flipperOffset, c - 14, midY + 6 + flipperOffset, paint)
    }

    private fun drawSpider(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawCircle(c.toFloat(), midY.toFloat(), 8f, paint)

        paint.color = PixelColor(30, 30, 30).toInt()
        canvas.drawRect(c - 2, midY - 12, c, midY - 6, paint)
        canvas.drawRect(c, midY - 12, c + 2, midY - 6, paint)
        canvas.drawRect(c - 2, midY + 6, c, midY + 12, paint)
        canvas.drawRect(c, midY + 6, c + 2, midY + 12, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawRect(c - 6, midY - 10, c - 3, midY - 4, paint)
        canvas.drawRect(c + 3, midY - 10, c + 6, midY - 4, paint)
        canvas.drawRect(c - 6, midY + 4, c - 3, midY + 10, paint)
        canvas.drawRect(c + 3, midY + 4, c + 6, midY + 10, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 4, midY - 2, 1.5f, paint)
        canvas.drawCircle(c + 4, midY - 2, 1.5f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 4, midY - 2, 0.8f, paint)
        canvas.drawCircle(c + 4, midY - 2, 0.8f, paint)

        paint.color = PixelColor(255, 0, 0).toInt()
        canvas.drawRect(c - 8, midY + 2, c - 6, midY + 4, paint)
        canvas.drawRect(c + 6, midY + 2, c + 8, midY + 4, paint)
    }

    private fun drawSpiderFrame(canvas: Canvas, paint: Paint, x: Int, y: Int, w: Int, h: Int, frame: Int) {
        val c = x + w / 2
        val midY = y + h / 2
        val crawl = if (frame % 2 == 0) 0 else 1

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawCircle(c.toFloat(), midY.toFloat(), 8f, paint)

        paint.color = PixelColor(30, 30, 30).toInt()
        canvas.drawRect(c - 2, midY - 12 + crawl, c, midY - 6 + crawl, paint)
        canvas.drawRect(c, midY - 12 - crawl, c + 2, midY - 6 - crawl, paint)
        canvas.drawRect(c - 2, midY + 6 - crawl, c, midY + 12 - crawl, paint)
        canvas.drawRect(c, midY + 6 + crawl, c + 2, midY + 12 + crawl, paint)

        paint.color = PixelColor(50, 50, 50).toInt()
        canvas.drawRect(c - 6, midY - 10 - crawl, c - 3, midY - 4 - crawl, paint)
        canvas.drawRect(c + 3, midY - 10 + crawl, c + 6, midY - 4 + crawl, paint)
        canvas.drawRect(c - 6, midY + 4 + crawl, c - 3, midY + 10 + crawl, paint)
        canvas.drawRect(c + 3, midY + 4 - crawl, c + 6, midY + 10 - crawl, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 4, midY - 2, 1.5f, paint)
        canvas.drawCircle(c + 4, midY - 2, 1.5f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 4, midY - 2, 0.8f, paint)
        canvas.drawCircle(c + 4, midY - 2, 0.8f, paint)

        paint.color = PixelColor(255, 0, 0).toInt()
        canvas.drawRect(c - 8, midY + 2, c - 6, midY + 4, paint)
        canvas.drawRect(c + 6, midY + 2, c + 8, midY + 4, paint)
    }

    private fun drawHuanHuan(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(255, 100, 150).toInt()
        canvas.drawRect(c - 8, midY - 10, c + 8, midY + 10, paint)

        paint.color = PixelColor(200, 50, 100).toInt()
        canvas.drawRect(c - 6, midY + 4, c + 6, midY + 10, paint)

        paint.color = PixelColor(255, 200, 220).toInt()
        canvas.drawRect(c - 4, midY - 2, c + 4, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 5, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 100, 150).toInt()
        canvas.drawRect(c - 10, midY - 14, c - 8, midY - 8, paint)
        canvas.drawRect(c + 8, midY - 14, c + 10, midY - 8, paint)

        paint.color = PixelColor(255, 200, 220).toInt()
        canvas.drawRect(c - 10, midY - 12, c - 8, midY - 10, paint)
        canvas.drawRect(c + 8, midY - 12, c + 10, midY - 10, paint)

        paint.color = PixelColor(255, 200, 220).toInt()
        canvas.drawRect(c - 12, midY + 6, c - 10, midY + 10, paint)
        canvas.drawRect(c + 10, midY + 6, c + 12, midY + 10, paint)
    }

    private fun drawLaLa(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(150, 100, 255).toInt()
        canvas.drawRect(c - 8, midY - 10, c + 8, midY + 10, paint)

        paint.color = PixelColor(100, 50, 200).toInt()
        canvas.drawRect(c - 6, midY + 4, c + 6, midY + 10, paint)

        paint.color = PixelColor(200, 180, 255).toInt()
        canvas.drawRect(c - 4, midY - 2, c + 4, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 5, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(150, 100, 255).toInt()
        canvas.drawRect(c - 12, midY - 6, c - 8, midY + 6, paint)
        canvas.drawRect(c + 8, midY - 6, c + 12, midY + 6, paint)

        paint.color = PixelColor(200, 180, 255).toInt()
        canvas.drawRect(c - 12, midY - 2, c - 8, midY + 2, paint)
        canvas.drawRect(c + 8, midY - 2, c + 12, midY + 2, paint)
    }

    private fun drawQiQi(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(100, 150, 255).toInt()
        canvas.drawRect(c - 8, midY - 10, c + 8, midY + 10, paint)

        paint.color = PixelColor(50, 100, 200).toInt()
        canvas.drawRect(c - 6, midY + 4, c + 6, midY + 10, paint)

        paint.color = PixelColor(180, 200, 255).toInt()
        canvas.drawRect(c - 4, midY - 2, c + 4, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 5, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawRect(c - 2, midY - 14, c + 2, midY - 8, paint)
        canvas.drawRect(c - 4, midY - 16, c + 4, midY - 14, paint)
    }

    private fun drawYouHa(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(255, 200, 50).toInt()
        canvas.drawRect(c - 8, midY - 10, c + 8, midY + 10, paint)

        paint.color = PixelColor(200, 150, 30).toInt()
        canvas.drawRect(c - 6, midY + 4, c + 6, midY + 10, paint)

        paint.color = PixelColor(255, 230, 150).toInt()
        canvas.drawRect(c - 4, midY - 2, c + 4, midY + 4, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 5, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 100, 50).toInt()
        canvas.drawRect(c - 12, midY - 6, c - 10, midY + 6, paint)
        canvas.drawRect(c + 10, midY - 6, c + 12, midY + 6, paint)
    }

    private fun drawDefaultPet(canvas: Canvas, paint: Paint, w: Int, h: Int) {
        val c = w / 2
        val midY = h / 2

        paint.color = PixelColor(200, 200, 200).toInt()
        canvas.drawRect(c - 8, midY - 10, c + 8, midY + 10, paint)

        paint.color = PixelColor(255, 255, 255).toInt()
        canvas.drawCircle(c - 5, midY - 6, 3f, paint)
        canvas.drawCircle(c + 5, midY - 6, 3f, paint)

        paint.color = PixelColor(0, 0, 0).toInt()
        canvas.drawCircle(c - 5, midY - 6, 1.5f, paint)
        canvas.drawCircle(c + 5, midY - 6, 1.5f, paint)

        paint.color = PixelColor(255, 150, 150).toInt()
        canvas.drawRect(c - 2, midY + 2, c + 2, midY + 4, paint)
    }
}
