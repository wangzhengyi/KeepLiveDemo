## app如何保证后台服务不被杀死

Android的进程保活包括两个方面：

1. 提高进程的优先级，降低进程被杀死的概率.
2. 在进程被杀死后，进行拉活.

### 进程优先级

按照进程的重要性，划分为5级：

1. 前台进程(Foreground process)
2. 可见进程(Visible process)
3. 服务进程(Service process)
4. 后台进程(Background process)
5. 空进程(Empty process)

#### 前台进程-Foreground process

用户当前操作所必需的进程。如果一个进程满足以下任一条件，即视为前台进程：

1. 托管用户正在交互的 Activity（已调用 Activity 的 onResume() 方法）
2. 托管某个 Service，后者绑定到用户正在交互的 Activity
3. 托管正在“前台”运行的 Service（服务已调用 startForeground()）
4. 托管正执行一个生命周期回调的 Service（onCreate()、onStart() 或 onDestroy()）
5. 托管正执行其 onReceive() 方法的 BroadcastReceiver

通常，在任意给定时间前台进程都为数不多。只有在内存不足以支持它们同时继续运行这一万不得已的情况下，系统才会终止它们。 此时，设备往往已达到内存分页状态，因此需要终止一些前台进程来确保用户界面正常响应。

#### 可见进程-Visible process

没有任何前台组件、但仍会影响用户在屏幕上所见内容的进程。如果一个进程满足以下任一条件，即视为可见进程：

1. 托管不在前台、但仍对用户可见的 Activity（已调用其 onPause() 方法）。例如，如果前台 Activity 启动了一个对话框，允许在其后显示上一 Activity，则有可能会发生这种情况。
2. 托管绑定到可见（或前台）Activity 的 Service。

可见进程被视为是极其重要的进程，除非为了维持所有前台进程同时运行而必须终止，否则系统不会终止这些进程。

#### 服务进程-Service process

正在运行已使用 startService() 方法启动的服务且不属于上述两个更高类别进程的进程。尽管服务进程与用户所见内容没有直接关联，但是它们通常在执行一些用户关心的操作（例如，在后台播放音乐或从网络下载数据）。因此，除非内存不足以维持所有前台进程和可见进程同时运行，否则系统会让服务进程保持运行状态。

#### 后台进程-Background process

包含目前对用户不可见的 Activity 的进程（已调用 Activity 的 onStop() 方法）。这些进程对用户体验没有直接影响，系统可能随时终止它们，以回收内存供前台进程、可见进程或服务进程使用。 通常会有很多后台进程在运行，因此它们会保存在 LRU （最近最少使用）列表中，以确保包含用户最近查看的 Activity 的进程最后一个被终止。如果某个 Activity 正确实现了生命周期方法，并保存了其当前状态，则终止其进程不会对用户体验产生明显影响，因为当用户导航回该 Activity 时，Activity 会恢复其所有可见状态。

#### 空进程-Empty process

不含任何活动应用组件的进程。保留这种进程的的唯一目的是用作缓存，以缩短下次在其中运行组件所需的启动时间。 为使总体系统资源在进程缓存和底层内核缓存之间保持平衡，系统往往会终止这些进程。

-------
### Android进程回收策略

Android中对于内存的回收，主要依靠LowMemoryKiller来完成，是一种根据OOM_ADJ阈值级别触发相应力度内存回收的机制.关于OOM_ADJ的说明如下:

| ADJ级别 | 取值 | 解释 |
| ------  | -----| ------|
| UNKNOWN_ADJ | 16 | 一般指将要会缓存进程，无法获取确定值|
| CACHED_APP_MAX_ADJ | 15 | 不可见进程的adj最大值 |
| CACHED_APP_MIN_ADJ | 9 | 不可见进程的adj最小值 |
| SERVICE_B_AD | 8 | B List中的Service |
| PREVIOUS_APP_ADJ | 7 | 上一个App的进程(往往通过按返回键)|
| HOME_APP_ADJ | 6 | Home进程 |
| SERVICE_ADJ | 5 | 服务进程(Service process) |
| HEAVY_WEIGHT_APP_ADJ | 4 | 后台的重量级进程, system/rootdir/init.rc文件中设置 |
| BACKUP_APP_ADJ | 3 | 备份进程 |
| PERCEPTIBLE_APP_ADJ | 2 | 可感知进程，比如后台音乐播放 |
| VISIBLE_APP_ADJ | 1 | 可见进程|
| FOREGROUND_APP_ADJ | 0 | 前台进程 |
| PERSISTENT_SERVICE_ADJ | -11 | 关联着系统或persistent进程 |
| PERSISTENT_PROC_ADJ | -12 | 系统persistent进程，比如telephony |
| SYSTEM_ADJ | -16 | 系统进程 |
| NATIVE_ADJ | -17 | native进程（不被系统管理）|

其中，OOM_ADJ >= 4的为比较容易被杀死的Android进程，0 <= OOM_ADJ <= 3的表示不容易被杀死的Android进程，其他表示非Android进程（纯Linux进程）.在Lowmemorykiller回收内存时会根据进程的优先级别杀死OOM_ADJ比较大的进程，对于优先级相同的进程则进一步受到进程所占内存和进程存活时间的影响.

Android手机中进程被杀死可能有如下情况：

| 进程杀死场景 | 调用接口 | 可能影响范围 |
| 触发系统进程管理机制 | Lowmemorykiller | 从进程importance值由大到小依次杀死，释放内存 |
| 被第三方应用杀死（无Root） | killBackgroundProcess | 只能杀死OOM_ADJ为4以上的进程 |
| 被第三方应用杀死（有Root） | force-stop或者kill|理论上可以杀死所有进程，一般只杀非系统关键进程和非前台和可见进程 |
| 厂商杀进程功能 | force-stop或者kill |
| 用户主动“强行停止”进程| force-stop | 只能停用第三方进程 |

综上，可以得出减少进程被杀死概率无非就是想办法提高进程优先级，减少进程在内存不足等情况下被杀死的概率.

--------
### 提升进程优先级的方案

#### 利用Activity提升权限

方案设计思想：监控手机锁屏解锁事件，在屏幕锁屏时启动一个像素的Activity，在用户解锁时将Activity销毁掉，注意该Activity需要设计成用户无感知.

目标：通过该方案，可以使进程的优先级在屏幕锁屏时间由4提高为1.

方案适用范围：

* 适用场景：本方案主要解决第三方应用以及系统管理工具在检测到锁屏事件后一段时机内会杀死后台进程，已达到省电的目的。
* 适用版本：适用所有的Android版本.

具体代码实现:

1. 定义一像素的Activity

```java
public class KeepLiveActivity extends Activity {
    public static final String TAG = KeepLiveActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: KeepLiveActivity->onCreate");
        KeepLiveManager.getInstance().initKeepLiveActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: KeepLiveActivity->onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: KeepLiveActivity->onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: KeepLiveActivity->onDestroy");
    }
}
```

2. 单例KeepLiveManager，用于初始化、启动、关闭一像素Activity

```java
public class KeepLiveManager {
    public static KeepLiveManager sInstance = new KeepLiveManager();
    public WeakReference<KeepLiveActivity> mWeakActivityRef = null;

    private KeepLiveManager() {

    }

    public static KeepLiveManager getInstance() {
        return sInstance;
    }

    public void initKeepLiveActivity(KeepLiveActivity keepLiveActivity) {
        this.mWeakActivityRef = new WeakReference<>(keepLiveActivity);

        // 设置1像素透明窗口
        Window window = keepLiveActivity.getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.width = 1;
        params.height = 1;
        window.setAttributes(params);
    }

    /**
     * 启动一像素Activity
     * @param context
     */
    public void startKeepLiveActivity(Context context) {
        Intent intent = new Intent(context, KeepLiveActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 结束一像素Activity
     */
    public void finishKeepLiveActivity() {
        if (mWeakActivityRef != null) {
            Activity activity = mWeakActivityRef.get();
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        }
    }
}
```


3. 动态注册Receiver，在灭屏时启动Activity，开屏时关闭Activity

```java
public class KeepLiveReceiver extends BroadcastReceiver {
    public static final String TAG = KeepLiveReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "onReceive: KeepLiveReceiver receive action =" + action);

        if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_USER_BACKGROUND.equals(action)) {
            KeepLiveManager.getInstance().startKeepLiveActivity(context);
        } else if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_USER_BACKGROUND.equals(action)) {
            KeepLiveManager.getInstance().finishKeepLiveActivity();
        }
    }
}
```


需要注意的是，开屏和灭屏广播需要动态注册，静态注册无效。

#### 利用Notification提升权限

方案设计思想：Android中Service的优先级为4，通过setForeground接口可以将后台Service设置为前台Service，使进程的优先级从4提升到2.

方案实现挑战：从Android2.3开始调用setForeground将后台Service设置为前台Service时，必须在系统通知栏发送一条通知，也就是前台Service与一条可见的通知绑定在一起.对于不需要常驻通知栏的应用来说，该方案虽好，但却是用户感知的，无法直接使用.

方案应对措施：通过实现一个内部Service，在KeepLiveService和其内部Service同时发生具有相同ID的Notification，然后将内部Service结束掉。随着内部Service的结束，Notification将会消失，但系统优先级依然保持为2.

具体代码实现：

1. 实现一个KeepLiveService，在onStartCommand里启动InnerService，注册当前服务为前台服务.

```java
public class KeepLiveService extends Service {
    private static final String TAG = KeepLiveService.class.getSimpleName();
    static KeepLiveService sKeepLiveService;
    public KeepLiveService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sKeepLiveService = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 模拟耗时操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Log.d(TAG, "run: do somethind waste time !!!");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        startService(new Intent(this, InnerService.class));
        return super.onStartCommand(intent, flags, startId);
    }

    public static class InnerService extends Service {

        @Override
        public int onStartCommand(Intent intent,int flags, int startId) {
            KeepLiveManager.getInstance().setForegroundService(sKeepLiveService, this);
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
```



2. KeepLiveManager中setForegroundService实现.

```java
/**
 * 提升Service的优先级为前台Service
 */
public void setForegroundService(final Service keepLiveService, final Service innerService) {
    final int foregroundPushId = 1;
    Log.d(TAG, "setForegroundService: KeepLiveService->setForegroundService: " + keepLiveService + ", innerService:" + innerService);
    if (keepLiveService != null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            keepLiveService.startForeground(foregroundPushId, new Notification());
        } else {
            keepLiveService.startForeground(foregroundPushId, new Notification());
            if (innerService != null) {
                innerService.startForeground(foregroundPushId, new Notification());
                innerService.stopSelf();
            }
        }
    }
}
```