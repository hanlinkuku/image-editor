package com.example.recyclerview.helper;// app/src/main/java/com/example/recyclerview/utils/EditHistoryManager.java

import java.util.ArrayList;
import java.util.List;

/**
 * 独立的历史记录管理器，负责 Undo/Redo 栈的维护与状态判断。
 * <p>
 * 预备用在 ImageInfoAdapter 中，用于实现 Undo/Redo 功能。
 */
public class EditHistoryManager {
    private static final int MAX_HISTORY_SIZE = 20;

    private final List<EditHistoryItem> mHistoryList = new ArrayList<>();
    private int mCurrentIndex = -1;

    // 内部类可移至包级（或保留为 static nested）
    public static class EditHistoryItem {
        public final int featureId;
        public final int subFeatureId;
        public final Object data;

        public EditHistoryItem(int featureId, int subFeatureId, Object data) {
            this.featureId = featureId;
            this.subFeatureId = subFeatureId;
            this.data = data;
        }
    }

    /** 保存新操作（自动截断 redo 分支 + 限长） */
    public void save(int featureId, int subFeatureId, Object data) {
        // 截断 redo 分支
        if (mCurrentIndex < mHistoryList.size() - 1) {
            mHistoryList.subList(mCurrentIndex + 1, mHistoryList.size()).clear();
        }

        mHistoryList.add(new EditHistoryItem(featureId, subFeatureId, data));
        mCurrentIndex = mHistoryList.size() - 1;

        // 限长 FIFO
        if (mHistoryList.size() > MAX_HISTORY_SIZE) {
            mHistoryList.remove(0);
            mCurrentIndex--;
        }
    }

    public boolean canUndo() {
        return mCurrentIndex >= 0;
    }

    public boolean canRedo() {
        return mCurrentIndex < mHistoryList.size() - 1;
    }

    public EditHistoryItem undo() {
        if (!canUndo()) return null;
        return mHistoryList.get(mCurrentIndex--);
    }

    public EditHistoryItem redo() {
        if (!canRedo()) return null;
        return mHistoryList.get(++mCurrentIndex);
    }

    public int getUndoCount() { return mCurrentIndex + 1; }
    public int getRedoCount() { return mHistoryList.size() - 1 - mCurrentIndex; }
}