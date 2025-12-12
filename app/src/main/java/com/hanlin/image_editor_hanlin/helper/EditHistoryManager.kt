package com.hanlin.image_editor_hanlin.helper

// app/src/main/java/com/example/recyclerview/utils/EditHistoryManager.java

/**
 * 独立的历史记录管理器，负责 Undo/Redo 栈的维护与状态判断。
 *
 *
 * 预备用在 ImageInfoAdapter 中，用于实现 Undo/Redo 功能。
 */
class EditHistoryManager {
    private val mHistoryList: MutableList<EditHistoryItem?> = ArrayList<EditHistoryItem?>()
    private var mCurrentIndex = -1

    // 内部类可移至包级（或保留为 static nested）
    class EditHistoryItem(@JvmField val featureId: Int, @JvmField val subFeatureId: Int, val data: Any?)

    /** 保存新操作（自动截断 redo 分支 + 限长）  */
    fun save(featureId: Int, subFeatureId: Int, data: Any?) {
        // 截断 redo 分支
        if (mCurrentIndex < mHistoryList.size - 1) {
            mHistoryList.subList(mCurrentIndex + 1, mHistoryList.size).clear()
        }

        mHistoryList.add(EditHistoryItem(featureId, subFeatureId, data))
        mCurrentIndex = mHistoryList.size - 1

        // 限长 FIFO
        if (mHistoryList.size > MAX_HISTORY_SIZE) {
            mHistoryList.removeAt(0)
            mCurrentIndex--
        }
    }

    fun canUndo(): Boolean {
        return mCurrentIndex >= 0
    }

    fun canRedo(): Boolean {
        return mCurrentIndex < mHistoryList.size - 1
    }

    fun undo(): EditHistoryItem? {
        if (!canUndo()) return null
        return mHistoryList.get(mCurrentIndex--)
    }

    fun redo(): EditHistoryItem? {
        if (!canRedo()) return null
        return mHistoryList.get(++mCurrentIndex)
    }

    val undoCount: Int
        get() = mCurrentIndex + 1
    val redoCount: Int
        get() = mHistoryList.size - 1 - mCurrentIndex

    companion object {
        private const val MAX_HISTORY_SIZE = 20
    }
}