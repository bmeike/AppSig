/* $Id: $
   Copyright 2015, G. Blake Meike

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package net.callmeike.android.buildid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipFile;


/**
 * Read the app signature from the APK.
 *
 * @author <a href="mailto:blake.meike@gmail.com">G. Blake Meike</a>
 * @version $Revision: $
 */
public final class AppSignatureTask extends AsyncTask<Class<? extends Activity>, Void, String> {
    private static final String TAG = "ID";

    public static final String MANIFEST = "META-INF/MANIFEST.MF";
    public static final String DEX = "classes.dex";
    public static final String SHA_HEADER = "SHA1-Digest: ";

    /** Callback contract */
    public static interface Callback { void onSignature(String signature); }


    private final Context ctxt;
    private final Callback callback;

    /**
     * Ctor: get the app signature.
     *
     * @param ctxt a context: held for the life of the task
     * @param callback called with the app digest
     */
    public AppSignatureTask(Context ctxt, Callback callback) {
        this.ctxt = ctxt.getApplicationContext();
        this.callback = callback;
    }

    /**
     * Find the apk file that contains the passed Activity.
     * The Activity class is used only to get a ComponentName
     * so that we can ask the PackageManager for the apk file
     *
     * @param act [0] is the application's main activity class
     * @return the apk signature
     */
    @SafeVarargs
    @Override
    protected final String doInBackground(Class<? extends Activity>... act) {
        String apk = getApk(act[0]);
        if (null == apk) { return null; }

        ZipFile zip = null;
        try {
            zip = new ZipFile(apk);
            return getDigest(zip);
        }
        catch (IOException e) {
            Log.e(TAG, "Failed opening manifest", e);
        }
        finally {
            if (null != zip) {
                try { zip.close(); } catch (Exception ignore) { }
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String sig) {
        if (null != sig) { callback.onSignature(sig); }
    }

    /**
     * Ask the PackageManager for the path to the apk that owns the passed activity class
     *
     * @param act the application's main activity class
     * @return the path to the apk that registered the activity
     */
    private String getApk(Class<? extends Activity> act) {
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setComponent(new ComponentName(ctxt, act));

        List<ResolveInfo> info = ctxt.getPackageManager().queryIntentActivities(i, 0);
        int n = info.size();
        if (1 > n) {
            Log.e(TAG, "component not found: " + act);
            return null;
        }

        if (1 < n) { Log.w(TAG, "too many components: " + n); }

        return info.get(0).activityInfo.applicationInfo.publicSourceDir;
    }

    /**
     * Read the apk zip manifest to find the signature for the DEX component.
     *
     * @param zip the app apk
     * @return the DEX component signature: "unknown" if not found
     */
    private String getDigest(ZipFile zip) {
        int state = 0;

        InputStream in = null;
        try {
            in = zip.getInputStream(zip.getEntry(MANIFEST));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                switch (state) {
                    case 0:
                        if (line.contains(DEX)) { state = 1; }
                        break;
                    case 1:
                        return (!line.contains(SHA_HEADER))
                            ? null
                            : line.substring(SHA_HEADER.length());
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Failed reading manifest", e);
        }
        finally {
            if (null != in) {
                try { in.close(); } catch (Exception ignore) { }
            }
        }

        return null;
    }
}
