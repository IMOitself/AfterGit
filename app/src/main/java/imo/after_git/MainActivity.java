package imo.after_git;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import imo.after_run.CommandTermux;

public class MainActivity extends Activity 
{
	ViewGroup instruction;
	TextView outputTxt;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		if(! hasStoragePermission()){
            requestStoragePermission();
            finish();
            return;
		}
		
		if(! CommandTermux.permissionIsGranted(this)){
			CommandTermux.permissionRequest(this);
			finish();
			return;
		}
		
		final EditText repoPathEdit = findViewById(R.id.repo_path_edittext);
		final Button repoLoadBtn = findViewById(R.id.repo_load_btn);
		instruction = findViewById(R.id.instruction);
		outputTxt = findViewById(R.id.output_txt);
		outputTxt.setMovementMethod(new ScrollingMovementMethod());
		
		instruction.setVisibility(View.GONE);
		
		repoLoadBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				if (! CommandTermux.backgroundMode) instruction.setVisibility(View.VISIBLE);
				String repoPath = repoPathEdit.getText().toString().trim();
				String command = "cd " + repoPath;
				command += "\ngit config --global --add safe.directory " + repoPath;
				command += "\ngit status";
				
				Runnable onDetect = new Runnable(){
					@Override
					public void run(){
						if (! CommandTermux.backgroundMode) instruction.setVisibility(View.GONE);
						outputTxt.setText(CommandTermux.OutputDetector.output);
					}
				};
				CommandTermux.OutputDetector.start(onDetect, MainActivity.this);
				CommandTermux.run(command, MainActivity.this);
			}
		});
    }
	
	
	boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
			requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }
}
