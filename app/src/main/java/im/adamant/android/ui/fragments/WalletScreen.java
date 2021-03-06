package im.adamant.android.ui.fragments;


import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;
import com.google.android.material.tabs.TabLayout;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import net.glxn.qrgen.android.QRCode;
import net.glxn.qrgen.core.image.ImageType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import im.adamant.android.R;
import im.adamant.android.Screens;
import im.adamant.android.ui.ShowQrCodeScreen;
import im.adamant.android.ui.entities.CurrencyTransferEntity;
import im.adamant.android.helpers.QrCodeHelper;
import im.adamant.android.ui.presenters.WalletPresenter;
import im.adamant.android.ui.adapters.CurrencyCardAdapter;
import im.adamant.android.ui.adapters.CurrencyTransfersAdapter;
import im.adamant.android.ui.transformations.ShadowTransformation;
import im.adamant.android.ui.entities.CurrencyCardItem;
import im.adamant.android.ui.mvp_view.WalletView;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */
public class WalletScreen extends BaseFragment implements WalletView {

    private CompositeDisposable subscriptions = new CompositeDisposable();

    @Inject
    @Named(Screens.WALLET_SCREEN)
    QrCodeHelper qrCodeHelper;

    @Inject
    Provider<WalletPresenter> presenterProvider;

    @Inject
    CurrencyCardAdapter currencyCardAdapter;

    @Inject
    CurrencyTransfersAdapter currencyTransfersAdapter;

    //--Moxy
    @InjectPresenter
    WalletPresenter presenter;

    @ProvidePresenter
    public WalletPresenter getPresenter(){
        return presenterProvider.get();
    }

    @BindView(R.id.fragment_wallet_tab_sliding_tabs) TabLayout tabs;
    @BindView(R.id.fragment_wallet_vp_swipe_slider) ViewPager slider;
    @BindView(R.id.fragment_wallet_rv_last_transactions) RecyclerView lastTransactions;

    public WalletScreen() {
        // Required empty public constructor
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_wallet_screen;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ShadowTransformation transformer = new ShadowTransformation(slider, currencyCardAdapter);
        slider.setAdapter(currencyCardAdapter);
        slider.setPageTransformer(false, transformer);
        slider.setOffscreenPageLimit(3);

        slider.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                CurrencyCardItem item = currencyCardAdapter.getItem(position);
                if (item != null){
                    presenter.onSelectCurrencyCard(item);
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabs.setupWithViewPager(slider);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this.getContext());
        lastTransactions.setLayoutManager(layoutManager);
        lastTransactions.setAdapter(currencyTransfersAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(lastTransactions.getContext(),
                ((LinearLayoutManager) layoutManager).getOrientation());
        lastTransactions.addItemDecoration(dividerItemDecoration);


        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        WeakReference<WalletPresenter> thisReference = new WeakReference<>(presenter);
        Disposable subscribe = currencyCardAdapter
                .getObservable()
                .subscribe(event -> {
                    WalletPresenter presenter = thisReference.get();
                    if (presenter == null){return;}

                    switch (event){
                        case COPY:
                            presenter.onClickCopyCurrentCardAddress();
                            break;
                        case CREATE_QR:
                            presenter.onClickCreateQrCodeCurrentCardAddress();
                            break;
                    }
                });

        subscriptions.add(subscribe);
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.onStopTransfersUpdate();
        subscriptions.dispose();
        subscriptions.clear();
    }

    @Override
    public void showCurrencyCards(List<CurrencyCardItem> currencyCardItems) {
        currencyCardAdapter.addCardItems(currencyCardItems);
    }

    @Override
    public void showLastTransfers(List<CurrencyTransferEntity> currencyTransferEntities) {
        currencyTransfersAdapter.refreshItems(currencyTransferEntities);
    }

    @Override
    public void putAddressToClipboard(String address) {
        Activity activity = getActivity();
        if (activity != null){
            ClipData clip = ClipData.newPlainText("addressView", address);
            ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);

            if(clipboard != null){
                clipboard.setPrimaryClip(clip);
                Toast.makeText(activity.getApplicationContext(), R.string.address_was_copied, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void createQrCode(String address) {
        Activity activity = getActivity();
        if (activity != null){
            Bundle bundle = new Bundle();
            bundle.putString(ShowQrCodeScreen.ARG_DATA_FOR_QR_CODE, address);

            Intent intent = new Intent(activity.getApplicationContext(), ShowQrCodeScreen.class);
            intent.putExtras(bundle);

            startActivity(intent);
        }
    }

}
