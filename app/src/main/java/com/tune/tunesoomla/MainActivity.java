package com.tune.tunesoomla;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.mobileapptracker.MATEvent;
import com.mobileapptracker.MATEventItem;
import com.mobileapptracker.MobileAppTracker;
import com.soomla.BusProvider;
import com.soomla.Soomla;
import com.soomla.profile.SoomlaProfile;
import com.soomla.profile.domain.IProvider;
import com.soomla.profile.domain.UserProfile;
import com.soomla.profile.events.auth.LoginFinishedEvent;
import com.soomla.profile.exceptions.ProviderNotFoundException;
import com.soomla.store.SoomlaStore;
import com.soomla.store.StoreInventory;
import com.soomla.store.billing.google.GooglePlayIabService;
import com.soomla.store.domain.MarketItem;
import com.soomla.store.events.MarketPurchaseEvent;
import com.soomla.store.exceptions.InsufficientFundsException;
import com.soomla.store.exceptions.VirtualItemNotFoundException;
import com.soomla.store.purchaseTypes.PurchaseWithMarket;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private MobileAppTracker mobileAppTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the TUNE SDK
        mobileAppTracker = MobileAppTracker.init(
                getApplicationContext(),
                "tune_advertiser_id",
                "tune_conversion_key");

        // Initialize Soomla
        Soomla.initialize("[YOUR CUSTOM GAME SECRET HERE]");
        // Initialize Soomla Profile
        SoomlaProfile.getInstance().initialize();
        // Initialize Soomla Store
        SoomlaStore.getInstance().initialize(new YourStoreAssetsImplementation());

        GooglePlayIabService.getInstance().setPublicKey("[YOUR PUBLIC KEY FROM GOOGLE PLAY]");
        GooglePlayIabService.AllowAndroidTestPurchases = true;

        // Initialize login button onclick listeners to perform login
        LoginListener loginListener = new LoginListener();
        findViewById(R.id.fbLogin).setOnClickListener(loginListener);
        findViewById(R.id.googleLogin).setOnClickListener(loginListener);
        findViewById(R.id.twitterLogin).setOnClickListener(loginListener);

        // Initialize buy button onclick listener to perform buy
        findViewById(R.id.buy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Buy the item, passing the item id and payload
                try {
                    StoreInventory.buy("item_id", "payload");
                } catch (InsufficientFundsException|VirtualItemNotFoundException e) {
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register to receive Soomla events
        BusProvider.getInstance().register(this);
        // Measure an app open in TUNE
        mobileAppTracker.setReferralSources(this);
        mobileAppTracker.measureSession();

        // Open IAB service (ideally when your store is opened)
        SoomlaStore.getInstance().startIabServiceInBg();
    }

    @Override
    protected void onPause() {
        // Close IAB service
        SoomlaStore.getInstance().stopIabServiceInBg();

        // Unregister from Soomla events
        BusProvider.getInstance().unregister(this);
        super.onPause();
    }

    // On purchase complete, set purchase info and measure purchase in TUNE
    @Subscribe
    public void onMarketPurchase(MarketPurchaseEvent marketPurchaseEvent) {
        double revenue;
        String currency;
        List<MATEventItem> items = new ArrayList<MATEventItem>();

        MarketItem item = ((PurchaseWithMarket) marketPurchaseEvent.getPurchasableVirtualItem().getPurchaseType()).getMarketItem();
        revenue = item.getMarketPriceMicros() / 1000000;
        currency = item.getMarketCurrencyCode();
        // Create event item to store purchase item data
        MATEventItem eventItem = new MATEventItem(item.getMarketTitle())
                .withAttribute1(item.getProductId());
        // Add event item to MATItem array in order to pass to TUNE SDK
        items.add(eventItem);

        // Get order ID and receipt data for purchase validation
        String orderId = marketPurchaseEvent.getOrderId();
        String receiptData = marketPurchaseEvent.getOriginalJson();
        String receiptSignature = marketPurchaseEvent.getSignature();

        // Create a MATEvent with this purchase data
        MATEvent purchaseEvent = new MATEvent(MATEvent.PURCHASE)
                .withRevenue(revenue)
                .withCurrencyCode(currency)
                .withAdvertiserRefId(orderId)
                .withReceipt(receiptData, receiptSignature);
        // Set event item if it exists
        if (!items.isEmpty()) {
            purchaseEvent.withEventItems(items);
        }
        // Measure "purchase" event
        mobileAppTracker.measureEvent(purchaseEvent);
    }

    // On login finished, set the user ID in TUNE based on provider, and measure a login
    @Subscribe
    public void onLoginFinished(LoginFinishedEvent loginFinishedEvent) {
        UserProfile user = loginFinishedEvent.UserProfile;
        if (user != null) {
            IProvider.Provider provider = user.getProvider();
            String userId = user.getProfileId();

            // Set different user IDs in TUNE SDK based on provider
            if (provider == IProvider.Provider.FACEBOOK) {
                mobileAppTracker.setFacebookUserId(userId);
            } else if (provider == IProvider.Provider.GOOGLE) {
                mobileAppTracker.setGoogleUserId(userId);
            } else if (provider == IProvider.Provider.TWITTER) {
                mobileAppTracker.setTwitterUserId(userId);
            } else {
                mobileAppTracker.setUserId(userId);
            }
            // Measure a login event for this user ID
            mobileAppTracker.measureEvent(MATEvent.LOGIN);
        }
    }

    // Custom OnClickListener that sets Provider based on button id
    public class LoginListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            IProvider.Provider provider;
            if (v.getId() == R.id.fbLogin) {
                provider = IProvider.Provider.FACEBOOK;
            } else if (v.getId() == R.id.googleLogin) {
                provider = IProvider.Provider.GOOGLE;
            } else {
                provider = IProvider.Provider.TWITTER;
            }

            try {
                SoomlaProfile.getInstance().login(MainActivity.this, provider);
            } catch (ProviderNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
