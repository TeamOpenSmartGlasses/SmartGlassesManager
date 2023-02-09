package SGMLib;

import android.content.Context;
import android.content.Intent;

public class SGMBroadcastSender {
    private String intentPkg;
    Context context;

    public SGMBroadcastSender(Context context) {
        this.context = context;
        this.intentPkg = this.context.getPackageName().contains(SGMData.SGMPkgName) ? "com.teamsmartglasses.to3pa" : "com.teamsmartglasses.from3pa";
        SGMData.sgmBroadcastSender = this;
    }

    public void broadcastData(String data) {
        Intent intent = new Intent();
        intent.putExtra("data", data);
        intent.setAction(intentPkg);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }
}