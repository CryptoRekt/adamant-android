package im.adamant.android.dagger;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import im.adamant.android.Screens;
import im.adamant.android.interactors.AuthorizeInteractor;
import im.adamant.android.interactors.ValidatePinCodeInteractor;
import im.adamant.android.presenters.LoginPresenter;
import im.adamant.android.presenters.PinCodePresenter;
import io.reactivex.disposables.CompositeDisposable;
import ru.terrakok.cicerone.Router;

@Module
public class PinCodeScreenModule {
    @ActivityScope
    @Provides
    public PinCodePresenter providePincodePresenter(
            ValidatePinCodeInteractor interactor,
            @Named(Screens.PINCODE_SCREEN) CompositeDisposable subscriptions
    ){
        return new PinCodePresenter(interactor,subscriptions);
    }

    @ActivityScope
    @Provides
    @Named(value = Screens.PINCODE_SCREEN)
    public CompositeDisposable provideComposite() {
        return new CompositeDisposable();
    }
}
