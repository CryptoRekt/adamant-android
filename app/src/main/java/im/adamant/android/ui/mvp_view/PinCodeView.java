package im.adamant.android.ui.mvp_view;

import com.arellomobile.mvp.MvpView;

public interface PinCodeView extends MvpView {
    enum MODE {
        VERIFY,
        CREATE
    }

    void setPinCodeMode(MODE mode);
}
