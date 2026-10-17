package ua.vodohrai.app;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.graphics.Color;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.os.Build;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.util.Base64;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
  public class ExportBridge {
    @JavascriptInterface public void saveBase64File(String filename,String mime,String payload){
      try {
        String safe=filename.replaceAll("[^\\p{L}\\p{N}._+ -]","_");
        if(safe.isEmpty()) safe="VodoHrai_export.csv";
        final String savedName=safe;
        byte[] bytes=Base64.decode(payload,Base64.DEFAULT);
        if(Build.VERSION.SDK_INT>=29){
          ContentValues values=new ContentValues();
          values.put(MediaStore.Downloads.DISPLAY_NAME,safe);
          values.put(MediaStore.Downloads.MIME_TYPE,mime);
          values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/VodoHrai");
          Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
          if(uri==null) throw new Exception("Cannot create download");
          try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new Exception("Cannot open download");out.write(bytes);}
        }else{
          File folder=getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
          if(folder==null)throw new Exception("No export directory");
          try(OutputStream out=new FileOutputStream(new File(folder,safe))){out.write(bytes);}
        }
        runOnUiThread(()->android.widget.Toast.makeText(MainActivity.this,"Файл збережено: "+savedName,android.widget.Toast.LENGTH_LONG).show());
      }catch(Exception e){runOnUiThread(()->android.widget.Toast.makeText(MainActivity.this,"Помилка експорту: "+e.getMessage(),android.widget.Toast.LENGTH_LONG).show());}
    }
  }
  private WebView web; private ValueCallback<Uri[]> chooser; private final int FILE_REQ=812;
  @Override public void onCreate(Bundle state){super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(153,27,27));
    FrameLayout frame=new FrameLayout(this); web=new WebView(this);frame.addView(web);
    ProgressBar progress=new ProgressBar(this); FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(100,100,android.view.Gravity.CENTER);frame.addView(progress,lp);
    setContentView(frame);
    WebSettings w=web.getSettings();w.setJavaScriptEnabled(true);w.setDomStorageEnabled(true);w.setAllowFileAccess(true);
    web.addJavascriptInterface(new ExportBridge(),"VodoHraiAndroid");
    w.setAllowFileAccessFromFileURLs(false);w.setAllowUniversalAccessFromFileURLs(false);w.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    web.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView v,String url){progress.setVisibility(View.GONE);} @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){Uri u=req.getUrl();String scheme=u.getScheme();if("http".equals(scheme)||"https".equals(scheme))return false;try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}});
    web.setWebChromeClient(new WebChromeClient(){@Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){if(chooser!=null)chooser.onReceiveValue(null);chooser=callback;try{startActivityForResult(params.createIntent(),FILE_REQ);return true;}catch(Exception e){chooser=null;return false;}}});
    web.setDownloadListener((url,userAgent,contentDisposition,mimeType,contentLength)->{
      if(url.startsWith("blob:")){android.widget.Toast.makeText(this,"Експорт через blob: відкрийте веб-версію для завантаження файлу",android.widget.Toast.LENGTH_LONG).show();return;}
      try{DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,contentDisposition,mimeType));((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);}catch(Exception e){android.widget.Toast.makeText(this,"Помилка завантаження",android.widget.Toast.LENGTH_SHORT).show();}
    });
    web.loadUrl("file:///android_asset/www/index.html");
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==FILE_REQ&&chooser!=null){chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));chooser=null;}}
  @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
