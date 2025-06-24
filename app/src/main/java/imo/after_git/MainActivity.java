package imo.after_git;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import imo.after_run.CommandTermux;
import java.util.Arrays;

public class MainActivity extends Activity 
{
    Button statusBtn;
    String repoPath = "";
    boolean isStop = false;
    boolean canRefreshStatus = false;
    
    static class gitStatusShort {
        static String branchStatus = "";
        static String[] filesStatus = {};
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		CommandTermux.checkAndRequestPermissions(this);
        
		
		final EditText repoPathEdit = findViewById(R.id.repo_path_edittext);
		statusBtn = findViewById(R.id.status_btn);
        final Button commitBtn = findViewById(R.id.commit_btn);
        final Button pullBtn = findViewById(R.id.pull_btn);
        final Button pushBtn = findViewById(R.id.push_btn);
		final TextView outputTxt = findViewById(R.id.output_txt);
        commitBtn.setVisibility(View.GONE);
        pullBtn.setVisibility(View.GONE);
        pushBtn.setVisibility(View.GONE);
        
		statusBtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
                commitBtn.setVisibility(View.GONE);
                pullBtn.setVisibility(View.GONE);
                pushBtn.setVisibility(View.GONE);
                
				repoPath = repoPathEdit.getText().toString().trim();
                
                Runnable onEnd = new Runnable(){
                    @Override
                    public void run(){
                        boolean doPull = gitStatusShort.branchStatus.contains("behind");
                        boolean doPush = gitStatusShort.branchStatus.contains("ahead");
                        boolean doCommit = gitStatusShort.filesStatus.length != 0;
                        
                        pullBtn.setVisibility(doPull ? View.VISIBLE : View.GONE);
                        pushBtn.setVisibility(doPush ? View.VISIBLE : View.GONE);
                        commitBtn.setVisibility(doCommit ? View.VISIBLE : View.GONE);
                    }
                };
                
                runGitStatus(repoPath, outputTxt, onEnd);
			}
		});
        
        commitBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    commitDialog(repoPath, gitStatusShort.filesStatus).show();
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
        if(!isStop || !canRefreshStatus) return;
        
        //update status
        statusBtn.performClick();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStop = true;
    }
    
    void runGitStatus(final String repoPath, final TextView outputTxt, final Runnable onEnd){
        final String commandDivider = "LONG STATUS ABOVE. SHORT STATUS BELOW.";
        String command = "cd " + repoPath;
        command += "\ngit status --long";
        command += "\necho \""+ commandDivider+"\"";
        command += "\ngit status --short --branch";

        new CommandTermux(command, MainActivity.this)
            .setOnEnd(new Runnable(){
                @Override
                public void run(){
                    String output = CommandTermux.getOutput();
                    
                    if(! isRepository(output, outputTxt)) return;
                    
                    String[] outputParts = output.split(commandDivider);
                    
                    String statusLong = outputParts[0];
                    String statusShort = outputParts[1];
                    
                    String[] statusShortParts = statusShort.trim().split("\n");
                    gitStatusShort.branchStatus = statusShortParts[0];
                    gitStatusShort.filesStatus = Arrays.copyOfRange(statusShortParts, 1, statusShortParts.length);
                    
                    outputTxt.setText(statusLong);
                    onEnd.run();
                }
            })
            .setLoading(outputTxt)
            .run();
    }
    
    boolean isRepository(String commandOutput, TextView textview){
        if(commandOutput.contains("cd: can't cd")){
            textview.setText("not a folder path");
            canRefreshStatus = false;
            return false;
        }
        if(commandOutput.contains("fatal: not a git repository")){
            textview.setText("not a git repository");
            canRefreshStatus = false;
            return false;
        }
        canRefreshStatus = true;
        return true;
    }
    
    AlertDialog commitDialog(final String repoPath, String[] changes){
        String title = "Commit Changes";
        LinearLayout layout = new LinearLayout(MainActivity.this);
        ListView changesList = new ListView(this);
        EditText commitMessageEdit = new EditText(this);
        CheckBox amendCheckbox = new CheckBox(this);
        CheckBox stageAllFilesCheckbox = new CheckBox(this);
        
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(changesList);
        layout.addView(commitMessageEdit);
        layout.addView(amendCheckbox);
        layout.addView(stageAllFilesCheckbox);
        
        changesList.setAdapter(new ArrayAdapter<>(
                                   MainActivity.this,
                                   android.R.layout.simple_list_item_1,
                                   gitStatusShort.filesStatus
                               ));
        commitMessageEdit.setHint("commit message...");
        amendCheckbox.setText("Amend previous commit");
        stageAllFilesCheckbox.setText("Stage all files");
        stageAllFilesCheckbox.setChecked(true);
        stageAllFilesCheckbox.setEnabled(false);// cannot be change
        
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
