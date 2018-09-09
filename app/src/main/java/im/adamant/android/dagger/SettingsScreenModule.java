package im.adamant.android.dagger;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import im.adamant.android.Screens;
import im.adamant.android.helpers.Settings;
import im.adamant.android.interactors.SaveKeypairInteractor;
import im.adamant.android.interactors.ServerNodeInteractor;
import im.adamant.android.interactors.SubscribeToPushInteractor;
import im.adamant.android.presenters.SettingsPresenter;
import im.adamant.android.ui.adapters.ServerNodeAdapter;
import io.reactivex.disposables.CompositeDisposable;
import ru.terrakok.cicerone.Router;

@Module
public class SettingsScreenModule {

    @FragmentScope
    @Provides
    public static SettingsPresenter provideSettingsPresenter(
            Settings settings,
            ServerNodeInteractor serverNodeInteractor,
            Router router,
            @Named(value = Screens.SETTINGS_SCREEN) CompositeDisposable subscriptions
    ) {
        return new SettingsPresenter(
                settings,
                serverNodeInteractor,
                router,
                subscriptions
        );
    }

    @FragmentScope
    @Provides
    @Named(value = Screens.SETTINGS_SCREEN)
    public CompositeDisposable provideComposite() {
        return new CompositeDisposable();
    }

    @FragmentScope
    @Provides
    public ServerNodeAdapter provideAdapter(Settings settings){
        return new ServerNodeAdapter(settings.getNodes());
    }
}
