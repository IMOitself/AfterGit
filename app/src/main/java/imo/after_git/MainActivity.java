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
import java.io.File;
import android.widget.CheckBox;

public class MainActivity extends Activity 
{
	TextView outputTxt;
    String repoPath = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		CommandTermux.checkAndRequestPermissions(this);
        
		
		final EditText repoPathEdit = findViewById(R.id.repo_path_edittext);
		final Button statusBtn = findViewById(R.id.status_btn);
        final Button commitBtn = findViewById(R.id.commit_btn);
        final Button pullBtn = findViewById(R.id.pull_btn);
        final Button pushBtn = findViewById(R.id.push_btn);
		outputTxt = findViewById(R.id.output_txt);
        commitBtn.setVisibility(View.GONE);
        pullBtn.setVisibility(View.GONE);
        pushBtn.setVisibility(View.GONE);
        
		statusBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				repoPath = repoPathEdit.getText().toString().trim();
                
                Runnable onEnd = new Runnable(){
                    @Override
                    public void run(){
                        String gitStatus = outputTxt.getText().toString();
                        if(gitStatus.contains("nothing to commit")){
                            commitBtn.setVisibility(View.GONE);
                        }else{
                            commitBtn.setVisibility(View.VISIBLE);
                        }
                        if(gitStatus.contains("branch is ahead of")){
                            pushBtn.setVisibility(View.VISIBLE);
                        }else{
                            pushBtn.setVisibility(View.GONE);
                        }
                        if(gitStatus.contains("branch is behind of")){
                            pullBtn.setVisibility(View.VISIBLE);
                        }else{
                            pullBtn.setVisibility(View.GONE);
                        }
                    }
                };
				runGitStatus(repoPath, outputTxt, onEnd);
			}
		});
        
        commitBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    //TODO: Show dialog with change list, edit commit message and commit button
                    commitDialog(repoPath).show();
                }
            });
            
        pullBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    //TODO: run git pull only if repo has commits behind
                    if(repoPath.isEmpty()) return;
                    
                }
            });
            
        pushBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    //TODO: run git push only if repo has commits ahead
                    if(repoPath.isEmpty()) return;
                    
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        outputTxt.setText("click status button to load repository folder");
    }
    
    void runGitStatus(final String repoPath, final TextView outputTxt, final Runnable onEnd){
        String command = "cd " + repoPath;
        command += "\ngit status";

        outputTxt.setText("wait..");
        new CommandTermux(command, MainActivity.this)
            .setOnDetect(new Runnable(){
                @Override
                public void run(){
                    String output = CommandTermux.getOutput();
                    outputTxt.setText(output);
                    onEnd.run();

                    boolean isWorking = output.contains("On branch");

                    if(! isWorking) fixGit(output, repoPath);
                }
            })
            .setOnCancel(new Runnable(){
                @Override
                public void run(){
                    outputTxt.setText("try again");
                }
            })
            .run();
    }
    
    AlertDialog commitDialog(final String repoPath){
        String title = "Commit";
        LinearLayout layout = new LinearLayout(MainActivity.this);
        final TextView changesText = new TextView(this);
        EditText commitMessageEdit = new EditText(this);
        CheckBox amendCheckbox = new CheckBox(this);
        CheckBox stageAllFilesCheckbox = new CheckBox(this);
        
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(changesText);
        layout.addView(commitMessageEdit);
        layout.addView(amendCheckbox);
        layout.addView(stageAllFilesCheckbox);
        
        commitMessageEdit.setHint("commit message...");
        amendCheckbox.setText("Amend previous commit");
        stageAllFilesCheckbox.setText("Stage all files");
        stageAllFilesCheckbox.setChecked(true);
        stageAllFilesCheckbox.setEnabled(false);// cannot be change
        
        String command = "cd " + repoPath;
        command += "\ngit status -s";
        new CommandTermux(command, MainActivity.this)
            .setOnDetect(new Runnable(){
                @Override
                public void run(){
                    changesText.setText(CommandTermux.getOutput());
                }
            })
            .run();
        
        return new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    dia.dismiss();
                }
            })
            .setPositiveButton("Commit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    
                }
            })
            .create();
    }
    
    void fixGit(final String output, String repoPath){
        String dialogTitle = "";
        String dialogMessage = "";
        String stringToCopy = "";
        
        if(output.contains("git: not found")) {
            dialogTitle = "Git not installed";
            stringToCopy = "pkg install git -y && exit";
            dialogMessage = "paste this on Termux to install git:\n\n" + stringToCopy;
        }
        else
        if(output.contains("dubious ownership")){
            dialogTitle = "Repo not listed in safe directories";
            stringToCopy = "git config --global --add safe.directory " + repoPath + " && exit";
            dialogMessage = "paste this on Termux to add repo as safe directory:\n\n" + stringToCopy;
        }
        else{
            return;
        }
        
        LinearLayout messageLayout = new LinearLayout(MainActivity.this);
        TextView messageText = new TextView(MainActivity.this);
        Button copyBtn = new Button(MainActivity.this);

        messageText.setText(dialogMessage);
        copyBtn.setText("Copy");
        final String copyString = stringToCopy;
        
        copyBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard == null) return;
                    clipboard.setPrimaryClip(ClipData.newPlainText("Copied Text", copyString));

                    Toast.makeText(MainActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });

        messageLayout.setOrientation(LinearLayout.VERTICAL);
        messageLayout.addView(messageText);
        messageLayout.addView(copyBtn);

        new AlertDialog.Builder(MainActivity.this)
            .setTitle(dialogTitle)
            .setView(messageLayout)
            .setPositiveButton("Open Termux", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    try {
                        CommandTermux.openTermux(MainActivity.this);
                    } catch(Exception e) {}
                }
            })
            .create().show();
    }
}
