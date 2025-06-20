package imo.after_git;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import imo.after_run.CommandTermux;
import android.widget.LinearLayout;

public class MainActivity extends Activity 
{
	ViewGroup instruction;
	TextView outputTxt;
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		CommandTermux.checkAndRequestPermissions(this);
        
		
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
				command += "\ngit status";
				
				outputTxt.setText("wait..");
                new CommandTermux(command, MainActivity.this)
                    .setOnDetect(new Runnable(){
                        @Override
                        public void run(){
                            String output = CommandTermux.OutputDetector.output;
                            outputTxt.setText(output);
                            
                            boolean isWorking = output.contains("On branch");
                            
                            if(isWorking) return;
                            
                            if(output.contains("git: not found")) {
                                LinearLayout messageLayout = new LinearLayout(MainActivity.this);
                                TextView messageText = new TextView(MainActivity.this);
                                Button copyBtn = new Button(MainActivity.this);
                                
                                messageText.setText("type this on Termux to install git:\n\npkg install git -y");
                                copyBtn.setOnClickListener(new View.OnClickListener(){
                                    @Override
                                    public void onClick(View v){
                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clipboard == null) return;
                                        ClipData clip = ClipData.newPlainText("Copied Text", "pkg install git -y");
                                        clipboard.setPrimaryClip(clip);

                                        Toast.makeText(MainActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                
                                messageLayout.setOrientation(LinearLayout.VERTICAL);
                                messageLayout.addView(messageText);
                                messageLayout.addView(copyBtn);
                                
                                new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Git not installed")
                                    .setView(messageLayout)
                                    .setNegativeButton("Open Termux", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dia, int which) {
                                            try {
                                                CommandTermux.openTermux(MainActivity.this);
                                            } catch(Exception e) {}
                                        }
                                    })
                                    .create().show();
                                return;
                            }

                            //TODO: run command "\ngit config --global --add safe.directory "
                            //      if current repoPath is not set yet
                        }
                    })
                    .run();
			}
		});
    }
}
