/**

 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.trent.kamera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.tedpark.tedpermission.rx2.TedRx2Permission
import com.trent.kamera.kamera.KameraActivity
import kotlinx.android.synthetic.main.activity_permission.*
import kotlin.reflect.KClass
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

/**
 * Activity for handling permissions.
 *

 */
class PermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        getPermission(this)

        start_preview.setOnClickListener { this.openActivity(KameraActivity::class) }
        view_license.setOnClickListener { this.openActivity(OssLicensesMenuActivity::class) }
    }

    private fun getPermission(context: Context) {
        TedRx2Permission.with(context)
                .setPermissions(android.Manifest.permission.CAMERA)
                .request()
                .subscribe({ result ->
                    if (result.isGranted) {
                        Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Permission Denied\n" + result.deniedPermissions, Toast.LENGTH_SHORT).show()
                    }
                })
    }

    private fun Context.openActivity(target: KClass<out Activity>) {
        this.startActivity(Intent(this, target.java))
    }
}