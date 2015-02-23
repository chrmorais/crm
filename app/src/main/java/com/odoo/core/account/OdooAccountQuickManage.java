/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 *
 * Created on 16/2/15 12:52 PM
 */
package com.odoo.core.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.odoo.core.auth.OdooAccountManager;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.support.OUser;
import com.odoo.core.support.OdooLoginHelper;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.logger.OLog;
import com.odoo.core.utils.notification.ONotificationBuilder;
import com.odoo.crm.R;

public class OdooAccountQuickManage extends ActionBarActivity implements View.OnClickListener {
    public static final String TAG = OdooAccountQuickManage.class.getSimpleName();
    private OUser mUser = null;
    private ImageView userAvatar;
    private TextView txvName;
    private Account account;
    private OdooLoginHelper loginHelper;
    private LoginProcess loginProcess = null;
    private EditText edtPassword;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.base_account_quick_manage);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getSupportActionBar().hide();
        action = getIntent().getAction();
        OLog.log(">>>  " + action);
        // Removing notification
        ONotificationBuilder.cancelNotification(this, OSyncAdapter.REQUEST_SIGN_IN_ERROR);
        mUser = OdooAccountManager.getDetails(this, getIntent().getStringExtra("android_name"));
        if (action.equals("remove_account")) {
            removePassword();
        } else {
            init();
            findViewById(R.id.cancel).setOnClickListener(this);
            findViewById(R.id.save_password).setOnClickListener(this);
        }
    }

    private void init() {
        userAvatar = (ImageView) findViewById(R.id.userAvatar);
        Bitmap userImage = BitmapUtils.getAlphabetImage(this, mUser.getName());
        if (!mUser.getAvatar().equals("false")) {
            userImage = BitmapUtils.getBitmapImage(this, mUser.getAvatar());
        }
        userAvatar.setImageBitmap(userImage);
        txvName = (TextView) findViewById(R.id.userName);
        txvName.setText(mUser.getName());
        Account[] accounts = AccountManager.get(getApplicationContext()).getAccountsByType(OdooAccountManager.KEY_ACCOUNT_TYPE);
        account = accounts[0];
    }

    @Override
    public void onClick(View v) {
        ONotificationBuilder.cancelNotification(this, OSyncAdapter.REQUEST_SIGN_IN_ERROR);
        switch (v.getId()) {
            case R.id.cancel:
                finish();
                break;
            case R.id.save_password:
                savePassword();
                break;
        }
    }

    private void removePassword() {
        OdooAccountManager.removeAccount(getApplicationContext(), account.name);
        finish();
    }

    private void savePassword() {
        edtPassword = (EditText) findViewById(R.id.newPassword);
        edtPassword.setError(null);
        if (TextUtils.isEmpty(edtPassword.getText())) {
            edtPassword.setError("Password required");
            edtPassword.requestFocus();
        }
        mUser.setPassword(edtPassword.getText().toString());
        loginProcess = new LoginProcess();
        loginHelper = new OdooLoginHelper(getApplicationContext());
        loginProcess.execute(mUser.getDBName(), mUser.getHost());
    }

    private class LoginProcess extends AsyncTask<String, Void, OUser> {


        @Override
        protected OUser doInBackground(String... params) {

            try {
                return loginHelper.login(mUser.getUsername(), mUser.getPassword(), mUser.getDatabase(), params[1], mUser.isAllowSelfSignedSSL());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(OUser oUser) {
            super.onPostExecute(oUser);
            if (oUser != null) {
                // Valid Login
                OdooAccountManager.updateUserData(OdooAccountQuickManage.this, mUser);
                finish();
            } else {
                edtPassword.setText("");
                edtPassword.setError("Password required");
                // Invalid password
            }
        }
    }
}
