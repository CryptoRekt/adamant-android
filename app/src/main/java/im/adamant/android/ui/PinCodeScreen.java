package im.adamant.android.ui;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Window;
import android.view.WindowManager;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockView;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.android.AndroidInjection;
import im.adamant.android.R;
import im.adamant.android.presenters.PinCodePresenter;
import im.adamant.android.ui.mvp_view.PinCodeView;

public class PinCodeScreen extends BaseActivity implements PinCodeView {
    public static final String ARG_MODE = "mode";

    @Inject
    Provider<PinCodePresenter> presenterProvider;

    //--Moxy
    @InjectPresenter
    PinCodePresenter presenter;

    @ProvidePresenter
    public PinCodePresenter getPresenter(){
        return presenterProvider.get();
    }

    private PinLockView mPinLockView;
    private IndicatorDots mIndicatorDots;

    @Override
    public int getLayoutId() {
        return R.layout.activity_pin_code_screen;
    }

    @Override
    public boolean withBackButton() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        mPinLockView = (PinLockView) findViewById(R.id.pin_lock_view);
        mIndicatorDots = (IndicatorDots) findViewById(R.id.indicator_dots);

        mPinLockView.attachIndicatorDots(mIndicatorDots);
//        mPinLockView.setPinLockListener(mPinLockListener);


        //mPinLockView.setCustomKeySet(new int[]{2, 3, 1, 5, 9, 6, 7, 0, 8, 4});
        //mPinLockView.enableLayoutShuffling();

        mPinLockView.setPinLength(4);

        mIndicatorDots.setIndicatorType(IndicatorDots.IndicatorType.FILL_WITH_ANIMATION);

    }

    @Override
    public void setPinCodeMode(MODE mode) {

    }
}
