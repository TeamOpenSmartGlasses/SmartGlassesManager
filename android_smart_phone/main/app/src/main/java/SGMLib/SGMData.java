package SGMLib;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SGMData {
    public static final String SGMPkgName = "com.wearableintelligencesystem.androidsmartphone";
    public static SGMBroadcastSender sgmBroadcastSender;
    public static PublishSubject<String> sgmOnData;
    public static Disposable sgmOnDataSubscription;
}
