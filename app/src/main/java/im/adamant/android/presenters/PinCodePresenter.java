package im.adamant.android.presenters;

import android.os.Bundle;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;

import im.adamant.android.R;
import im.adamant.android.core.exceptions.PincodeInvalidException;
import im.adamant.android.helpers.LoggerHelper;
import im.adamant.android.interactors.ValidatePinCodeInteractor;
import im.adamant.android.ui.mvp_view.PinCodeView;
import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@InjectViewState
public class PinCodePresenter extends BasePresenter<PinCodeView> {
    private ValidatePinCodeInteractor pinCodeInteractor;
    private PinCodeView.MODE mode = PinCodeView.MODE.VERIFY;

    public PinCodePresenter(ValidatePinCodeInteractor pinCodeInteractor, CompositeDisposable subscriptions) {
        super(subscriptions);
        this.pinCodeInteractor = pinCodeInteractor;
    }

    public void setMode(PinCodeView.MODE mode) {
        this.mode = mode;
        switch (mode){
            case CREATE: {
                getViewState().setSuggestion(R.string.activity_pincode_enter_new_pincode);
            }
            break;
            case VERIFY: {
                getViewState().setSuggestion(R.string.activity_pincode_enter_pincode);
            }
            break;
        }
    }

    public void onInputPincodeWasCompleted(String pinCode) {
        PinCodeView viewState = getViewState();
        switch (mode){
            case CREATE: {
                Disposable subscription = pinCodeInteractor
                        .createPincode(pinCode)
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                                () -> {
                                    Bundle bundle = new Bundle();
                                    bundle.putBoolean(PinCodeView.ARG_CREATED, true);
                                    viewState.close(bundle);
                                },
                                error -> LoggerHelper.e("PINCODE", error.getMessage(), error)
                        );
                subscriptions.add(subscription);
            }
            break;
            case VERIFY: {
                Disposable subscription = pinCodeInteractor
                        .verifyPincode(pinCode)
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                                () -> {
                                    Bundle bundle = new Bundle();
                                    bundle.putBoolean(PinCodeView.ARG_VERIFIED, true);
                                    viewState.close(bundle);
                                },
                                error -> {
                                    if (error instanceof PincodeInvalidException) {
                                        PincodeInvalidException invalidException = (PincodeInvalidException)error;
                                        switch (invalidException.getReason()){
                                            case NOT_MATCH: {
                                                viewState.showError(R.string.wrong_pincode);
                                            }
                                            break;
                                        }
                                    }
                                    LoggerHelper.e("PINCODE", error.getMessage(), error);
                                }
                        );
                subscriptions.add(subscription);
            }
            break;
        }
    }
}
