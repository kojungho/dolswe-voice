package co.kr.badara.dolswevoice;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
public final class UpdateApkProvider extends ContentProvider {
 static File file(Context c){return new File(c.getFilesDir(),"updates/dolswe-voice-update.apk");}
 private void check(Uri uri)throws FileNotFoundException{if(!"/update.apk".equals(uri.getPath())||uri.getQuery()!=null||uri.getFragment()!=null)throw new FileNotFoundException();}
 public boolean onCreate(){return true;}public String getType(Uri uri){return "application/vnd.android.package-archive";}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{check(uri);}catch(Exception e){return null;}String[] names=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(names);Object[] values=new Object[names.length];for(int i=0;i<names.length;i++){if(names[i].equals(OpenableColumns.DISPLAY_NAME))values[i]="dolswe-voice-update.apk";if(names[i].equals(OpenableColumns.SIZE))values[i]=file(getContext()).length();}c.addRow(values);return c;}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{check(uri);if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(getContext()),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String w,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String w,String[] a){throw new UnsupportedOperationException();}
}
