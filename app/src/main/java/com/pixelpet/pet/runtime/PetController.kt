package com.pixelpet.pet.runtime

import com.pixelpet.pet.view.PetView

/**
 * 连接 [PetRuntime]（状态机）与 [PetView]（渲染）的控制器。
 *
 * OverlayWindowManager 通过本类驱动宠物的手势响应与每帧刷新；
 * PetDetailActivity 目前直接使用 PetRuntime，后续可统一走本类以保持行为一致。
 */
class PetController(
    private val petView: PetView,
    private val petRuntime: PetRuntime
) {
    /** 每帧调用：推进状态机并把最新状态推给视图。 */
    fun update() {
        petRuntime.tick(System.currentTimeMillis())
        petView.updateState(petRuntime.state)
    }

    fun onTap() {
        petRuntime.handleTap(System.currentTimeMillis())
        update()
    }

    fun onDoubleTap() {
        petRuntime.handleDoubleTap(System.currentTimeMillis())
        update()
    }

    fun onLongPress() {
        petRuntime.handleLongPressStart()
        update()
    }

    fun onRelease() {
        petRuntime.handleRelease()
        update()
    }
}
