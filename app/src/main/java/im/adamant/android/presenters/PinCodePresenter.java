package im.adamant.android.presenters;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;

import im.adamant.android.interactors.ValidatePinCodeInteractor;
import im.adamant.android.ui.mvp_view.PinCodeView;
import io.reactivex.disposables.CompositeDisposable;

@InjectViewState
public class PinCodePresenter extends MvpPresenter<PinCodeView> {
    private ValidatePinCodeInteractor pinCodeInteractor;
    private CompositeDisposable subscriptions;

    public PinCodePresenter(ValidatePinCodeInteractor pinCodeInteractor, CompositeDisposable subscriptions) {
        this.pinCodeInteractor = pinCodeInteractor;
        this.subscriptions = subscriptions;
    }
}
