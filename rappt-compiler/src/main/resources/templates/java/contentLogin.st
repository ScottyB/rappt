contentLogin(loginData) ::= <<

    @Pref
	  $loginData.prefName$_ prefs;

    ProgressDialog progress;

    @ViewById
    EditText email, password;

    @App
    $loginData.applicationName$ app;

    String userEmail;

    @Background
    void performLogin(String email, String password) {
        $loginData.authDataClass$ authData = new $loginData.authDataClass$();
        UserData user = new UserData();
        user.email = email;
        user.password = password;
        authData.user = user;
        try {
            afterLogin(app.dataStore.$loginData.authFunction$(authData));
        } catch (Exception e) {
            ErrorDialog.showError(this, e);
            progress.dismiss();
        }
    }

    @Click
    void loginId() {
        String password = this.password.getText().toString().trim();
        userEmail = this.email.getText().toString().trim();
        progress = new ProgressDialog(this, AlertDialog.THEME_HOLO_LIGHT);
        progress.setTitle("Please Wait");
        progress.setMessage("Signing in");
        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();
        performLogin(userEmail, password);
    }

    @UiThread
    void afterLogin($loginData.authDataClass$ authData) {
        prefs.$loginData.tokenPrefName$().put($(loginData.tokenPref.templateName)(loginData.tokenPref)$);
        prefs.email().put(userEmail);
        progress.dismiss();
        nextScreen();
    }

    void nextScreen() {
        $loginData.nextScreen$_.intent(this).start();
        finish();
   }

    @AfterViews
    void afterViews() {
         if (prefs.$loginData.tokenPrefName$().exists()) {
             nextScreen();
         }
         if (prefs.email().exists()) {
             email.setText(prefs.email().get());
             password.requestFocus();
         }
    }

>>
