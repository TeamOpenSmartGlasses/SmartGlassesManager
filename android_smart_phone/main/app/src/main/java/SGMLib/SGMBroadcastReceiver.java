package SGMLib;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import com.wearableintelligencesystem.androidsmartphone.WearableAiAspService;

import java.util.concurrent.Callable;
import java.util.function.Function;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class SGMBroadcastReceiver extends BroadcastReceiver {
    private String filterPkg;
    private Context context;
    public String TAG = "HELLO123";

    public SGMBroadcastReceiver(Context myContext) {
        this.context = myContext;
        Log.d(TAG, "PKGFILTER: 1");
        SGMData.sgmOnData = PublishSubject.create();
        Log.d(TAG, "log: ctxpkgn: " + this.context.getPackageName() + " sgmdat: " + SGMData.SGMPkgName);
        this.filterPkg = this.context.getPackageName().contains(SGMData.SGMPkgName) ? "com.teamopensmartglasses.from3pa" : "com.teamopensmartglasses.to3pa";
        Log.d(TAG, "PKGFILTER: " + this.filterPkg);
        IntentFilter intentFilter = new IntentFilter(this.filterPkg);
        this.context.registerReceiver(this, intentFilter);
        Log.d(TAG, "PKGFILTER: " + this.filterPkg);
    }

    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String data = bundle.getString("data");

        Log.i("Broadcastreceiver", "BroadcastReceiver Received");

        SGMData.sgmOnData.onNext(data);
        /*
        try {
            //For now, send generic card
            String testCardImg = "https://ichef.bbci.co.uk/news/976/cpsprodpb/7727/production/_103330503_musk3.jpg";
            String testCardTitle = "3pa title";
            String testCardContent = "Woah dude this is from a 3pa";

            this.mService.sendTestCard(testCardTitle, testCardContent, testCardImg);
        } catch (Exception e){
            e.printStackTrace();
        }
*/
    }
}
