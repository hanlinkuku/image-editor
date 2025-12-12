package com.example.recyclerview.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.example.recyclerview.entity.ImageInfoBean;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "ImageEditorApp";

    private static final String DATABASE_NAME = "image_editor_db";
    private static final int DATABASE_VERSION = 2;

    // 作品集表
    private static final String TABLE_PORTFOLIO = "portfolio";
    private static final String COLUMN_PORTFOLIO_ID = "portfolio_id";
    private static final String COLUMN_IMAGE_URI = "image_uri";
    private static final String COLUMN_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "✅ DatabaseHelper initialized with version: " + DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "✅ onCreate called");
        // 创建作品集表
        String CREATE_PORTFOLIO_TABLE = "CREATE TABLE " + TABLE_PORTFOLIO + "("
                + COLUMN_PORTFOLIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_IMAGE_URI + " TEXT NOT NULL,"
                + COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(CREATE_PORTFOLIO_TABLE);
        Log.d(TAG, "✅ Created portfolio table");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "🔄 Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            // 创建作品集表
            String CREATE_PORTFOLIO_TABLE = "CREATE TABLE " + TABLE_PORTFOLIO + "("
                    + COLUMN_PORTFOLIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_IMAGE_URI + " TEXT NOT NULL,"
                    + COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            db.execSQL(CREATE_PORTFOLIO_TABLE);
            Log.d(TAG, "✅ Created portfolio table in upgrade");
        }
    }



    // 添加作品集图片
    public long addPortfolioImage(String imageUri) {
        Log.d(TAG, "✅ Adding portfolio image: " + imageUri);
        
        if (imageUri == null || imageUri.isEmpty()) {
            Log.e(TAG, "❌ Image URI is null or empty");
            return -1;
        }
        
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            
            // 检查表是否存在
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", 
                                       new String[]{TABLE_PORTFOLIO});
            boolean tableExists = cursor.moveToFirst();
            cursor.close();
            Log.d(TAG, "📊 Table portfolio exists: " + tableExists);
            
            if (!tableExists) {
                // 表不存在，创建它
                Log.d(TAG, "✅ Creating portfolio table");
                String CREATE_PORTFOLIO_TABLE = "CREATE TABLE " + TABLE_PORTFOLIO + "("
                        + COLUMN_PORTFOLIO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + COLUMN_IMAGE_URI + " TEXT NOT NULL,"
                        + COLUMN_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
                db.execSQL(CREATE_PORTFOLIO_TABLE);
                Log.d(TAG, "✅ Portfolio table created");
            }
            
            ContentValues values = new ContentValues();
            values.put(COLUMN_IMAGE_URI, imageUri);
            
            long id = db.insert(TABLE_PORTFOLIO, null, values);
            if (id == -1) {
                Log.e(TAG, "❌ Insert failed: " + db.getPath() + ", URI: " + imageUri);
                // 获取更多错误信息
                String lastError = db.getPath() + " - Last error: " + db.getPath();
                Log.e(TAG, "❌ Database path: " + lastError);
            } else {
                Log.d(TAG, "✅ Added portfolio image with id: " + id);
            }
            
            return id;
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception in addPortfolioImage: " + e.getMessage(), e);
            return -1;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    // 获取所有作品集图片
    public List<ImageInfoBean> getAllPortfolioImages() {
        Log.d(TAG, "✅ Getting all portfolio images");
        List<ImageInfoBean> images = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PORTFOLIO, null, null, null, null, null, COLUMN_CREATED_AT + " DESC");
        
        Log.d(TAG, "✅ Number of portfolio images found: " + cursor.getCount());
        
        if (cursor.moveToFirst()) {
            do {
                int idColumnIndex = cursor.getColumnIndex(COLUMN_PORTFOLIO_ID);
                int uriColumnIndex = cursor.getColumnIndex(COLUMN_IMAGE_URI);
                
                if (idColumnIndex != -1 && uriColumnIndex != -1) {
                    long id = cursor.getLong(idColumnIndex);
                    String imageUri = cursor.getString(uriColumnIndex);
                    
                    Log.d(TAG, "✅ Found portfolio image: id=" + id + ", uri=" + imageUri);
                    
                    // 构造ImageInfoBean对象，与Gallery中使用的格式一致
                    ImageInfoBean imageInfo = new ImageInfoBean();
                    imageInfo.setId(id);
                    imageInfo.setPath(imageUri);
                    imageInfo.setName("Portfolio Image"); // 默认名称
                    imageInfo.setSize(0); // 默认大小
                    
                    images.add(imageInfo);
                } else {
                    Log.e(TAG, "❌ Column index not found. ID column index: " + idColumnIndex + ", URI column index: " + uriColumnIndex);
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return images;
    }

    // 删除作品集图片
    public void deletePortfolioImage(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PORTFOLIO, COLUMN_PORTFOLIO_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}