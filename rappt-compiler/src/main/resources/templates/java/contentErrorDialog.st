contentErrorDialog(data=false) ::= <<
    public static final String MSG = "msg";
    private Dialog mDialog;

    public static ErrorDialog newInstance(String msg) {
        ErrorDialog frag = new ErrorDialog();
        Bundle args = new Bundle();
        args.putString(MSG, msg);
        frag.setArguments(args);
        return frag;
    }

    private void setDialog(Dialog dialog) {
        mDialog = dialog;
    }

    $if(data.doesRequireGooglePlayServices)$
    static public boolean servicesConnected(FragmentActivity context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        return servicesConnected(context, resultCode);
    }

    static public boolean servicesConnected(FragmentActivity context, int resultCode) {
        if (ConnectionResult.SUCCESS != resultCode) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, context, 0);
            if (dialog != null) {
                ErrorDialog errorFragment = new ErrorDialog();
                errorFragment.setDialog(dialog);
                errorFragment.show(context.getSupportFragmentManager(), "PLAY_SERVICES");
            } else {
                showError(context, "Google Play could not show a dialog.");
            }
            return false;
        }
        return true;
    }
    $endif$

    public static void showError(FragmentActivity activity, String message) {
        ErrorDialog.newInstance(message).show(activity.getSupportFragmentManager(), "ErrorDialog");
    }

     @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDialog == null) {
            String msg = getArguments().getString(MSG);
            mDialog = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT)
                    .setTitle("Error")
                    .setMessage(msg)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
        return mDialog;
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void showError(FragmentActivity activity, Exception e){
        String message = e.getMessage() == null ? "": e.getMessage();
        if (!isNetworkAvailable(activity)) {
            message = activity.getResources().getString(R.string.no_internet_connection);
        } else if (message.contains("No authentication challenges found")) {
            message = activity.getResources().getString(R.string.wrong_password_email);
        } $if(data.hasRetrofit)$else if (e instanceof RetrofitError) {
            RetrofitError retrofitError = (RetrofitError)e;
            message = activity.getResources().getString(R.string.retrofit_error)
                    + " " + retrofitError.getUrl()
                    + " " + retrofitError.getResponse().getReason()
                    + " " + retrofitError.getResponse().getStatus();
            if (retrofitError.getResponse().getStatus() == 403) {
                message += "\nUsing https?";
            }
        }$endif$
        ErrorDialog.newInstance(message).show(activity.getSupportFragmentManager(), "ErrorDialog");
    }
>>
