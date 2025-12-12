package com.example.recyclerview.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.recyclerview.entity.ImageInfoBean

class DatabaseHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    init {
        Log.d(TAG, "✅ DatabaseHelper initialized with version: " + DATABASE_VERSION)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "✅ onCreate called")
        // 创建作品集表
        val CREATE_PORTFOLIO_TABLE = ("CREATE TABLE " + TABLE_PORTFOLIO + "("
                + COLUMN_PORTFOLIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_IMAGE_URI + " TEXT NOT NULL,"
                + COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
        db.execSQL(CREATE_PORTFOLIO_TABLE)
        Log.d(TAG, "✅ Created portfolio table")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "🔄 Upgrading database from version " + oldVersion + " to " + newVersion)
        if (oldVersion < 2) {
            // 创建作品集表
            val CREATE_PORTFOLIO_TABLE = ("CREATE TABLE " + TABLE_PORTFOLIO + "("
                    + COLUMN_PORTFOLIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_IMAGE_URI + " TEXT NOT NULL,"
                    + COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL(CREATE_PORTFOLIO_TABLE)
            Log.d(TAG, "✅ Created portfolio table in upgrade")
        }
    }


    // 添加作品集图片
    fun addPortfolioImage(imageUri: String?): Long {
        Log.d(TAG, "✅ Adding portfolio image: " + imageUri)

        if (imageUri == null || imageUri.isEmpty()) {
            Log.e(TAG, "❌ Image URI is null or empty")
            return -1
        }

        var db: SQLiteDatabase? = null
        try {
            db = this.getWritableDatabase()


            // 检查表是否存在
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf<String>(TABLE_PORTFOLIO)
            )
            val tableExists = cursor.moveToFirst()
            cursor.close()
            Log.d(TAG, "📊 Table portfolio exists: " + tableExists)

            if (!tableExists) {
                // 表不存在，创建它
                Log.d(TAG, "✅ Creating portfolio table")
                val CREATE_PORTFOLIO_TABLE = ("CREATE TABLE " + TABLE_PORTFOLIO + "("
                        + COLUMN_PORTFOLIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + COLUMN_IMAGE_URI + " TEXT NOT NULL,"
                        + COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
                db.execSQL(CREATE_PORTFOLIO_TABLE)
                Log.d(TAG, "✅ Portfolio table created")
            }

            val values = ContentValues()
            values.put(COLUMN_IMAGE_URI, imageUri)

            val id = db.insert(TABLE_PORTFOLIO, null, values)
            if (id == -1L) {
                Log.e(TAG, "❌ Insert failed: " + db.getPath() + ", URI: " + imageUri)
                // 获取更多错误信息
                val lastError = db.getPath() + " - Last error: " + db.getPath()
                Log.e(TAG, "❌ Database path: " + lastError)
            } else {
                Log.d(TAG, "✅ Added portfolio image with id: " + id)
            }

            return id
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in addPortfolioImage: " + e.message, e)
            return -1
        } finally {
            if (db != null && db.isOpen()) {
                db.close()
            }
        }
    }

    val allPortfolioImages: MutableList<ImageInfoBean?>
        // 获取所有作品集图片
        get() {
            Log.d(TAG, "✅ Getting all portfolio images")
            val images: MutableList<ImageInfoBean?> =
                ArrayList<ImageInfoBean?>()
            val db = this.getReadableDatabase()
            val cursor = db.query(
                TABLE_PORTFOLIO,
                null,
                null,
                null,
                null,
                null,
                COLUMN_CREATED_AT + " DESC"
            )

            Log.d(
                TAG,
                "✅ Number of portfolio images found: " + cursor.getCount()
            )

            if (cursor.moveToFirst()) {
                do {
                    val idColumnIndex =
                        cursor.getColumnIndex(COLUMN_PORTFOLIO_ID)
                    val uriColumnIndex =
                        cursor.getColumnIndex(COLUMN_IMAGE_URI)

                    if (idColumnIndex != -1 && uriColumnIndex != -1) {
                        val id = cursor.getLong(idColumnIndex)
                        val imageUri = cursor.getString(uriColumnIndex)

                        Log.d(
                            TAG,
                            "✅ Found portfolio image: id=" + id + ", uri=" + imageUri
                        )


                        // 构造ImageInfoBean对象，与Gallery中使用的格式一致
                        val imageInfo = ImageInfoBean()
                        imageInfo.id = id
                        imageInfo.path = imageUri
                        imageInfo.name = "Portfolio Image" // 默认名称
                        imageInfo.size = 0 // 默认大小

                        images.add(imageInfo)
                    } else {
                        Log.e(
                            TAG,
                            "❌ Column index not found. ID column index: " + idColumnIndex + ", URI column index: " + uriColumnIndex
                        )
                    }
                } while (cursor.moveToNext())
            }

            cursor.close()
            return images
        }

    // 删除作品集图片
    fun deletePortfolioImage(id: Long) {
        val db = this.getWritableDatabase()
        db.delete(TABLE_PORTFOLIO, COLUMN_PORTFOLIO_ID + " = ?", arrayOf<String>(id.toString()))
        db.close()
    }

    companion object {
        private const val TAG = "ImageEditorApp"

        private const val DATABASE_NAME = "image_editor_db"
        private const val DATABASE_VERSION = 2

        // 作品集表
        private const val TABLE_PORTFOLIO = "portfolio"
        private const val COLUMN_PORTFOLIO_ID = "portfolio_id"
        private const val COLUMN_IMAGE_URI = "image_uri"
        private const val COLUMN_CREATED_AT = "created_at"
    }
}