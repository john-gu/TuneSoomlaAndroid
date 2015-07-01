package com.tune.tunesoomla;

import com.soomla.store.IStoreAssets;
import com.soomla.store.domain.VirtualCategory;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrency;
import com.soomla.store.domain.virtualCurrencies.VirtualCurrencyPack;
import com.soomla.store.domain.virtualGoods.VirtualGood;

public class YourStoreAssetsImplementation implements IStoreAssets {
    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public VirtualCurrency[] getCurrencies() {
        return new VirtualCurrency[0];
    }

    @Override
    public VirtualGood[] getGoods() {
        return new VirtualGood[0];
    }

    @Override
    public VirtualCurrencyPack[] getCurrencyPacks() {
        return new VirtualCurrencyPack[0];
    }

    @Override
    public VirtualCategory[] getCategories() {
        return new VirtualCategory[0];
    }
}
