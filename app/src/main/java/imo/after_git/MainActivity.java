package imo.after_git;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import imo.after_run.CommandTermux;
import java.util.Arrays;
import android.widget.CompoundButton;

public class MainActivity extends Activity 
{
    Button statusBtn;
    String repoPath = "";
    boolean isStop = false;
    boolean canRefreshStatus = false;
    AlertDialog commitDialog;
    AlertDialog diffDialog;
    AlertDialog configDialog;
    
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
                    commitDialog = makeCommitDialog(repoPath, gitStatusShort.filesStatus);
                    commitDialog.show();
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
        statusBtn.performClick();//refresh status
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStop = true;
        if(commitDialog != null && commitDialog.isShowing())
            commitDialog.dismiss();
            
        if(diffDialog != null && diffDialog.isShowing())
            diffDialog.dismiss();
            
        if(configDialog != null && configDialog.isShowing())
            configDialog.dismiss();
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
                    boolean isWorking = output.contains("On branch");
                    if(! isWorking) fixGit(output, repoPath);
                    
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
    
    AlertDialog makeCommitDialog(final String repoPath, String[] changes){
        String title = "Commit Changes";
        LinearLayout layout = new LinearLayout(this);
        ListView changesList = new ListView(this);
        final EditText commitMessageEdit = new EditText(this);
        CheckBox amendCheckbox = new CheckBox(this);
        CheckBox stageAllFilesCheckbox = new CheckBox(this);
        
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(changesList);
        layout.addView(commitMessageEdit);
        layout.addView(amendCheckbox);
        layout.addView(stageAllFilesCheckbox);
        
        changesList.setAdapter(new CommitChangesAdapter(MainActivity.this, repoPath, changes));
        commitMessageEdit.setHint("commit message...");
        amendCheckbox.setText("Amend previous commit");
        stageAllFilesCheckbox.setText("Stage all files");
        stageAllFilesCheckbox.setChecked(true);
        stageAllFilesCheckbox.setEnabled(false);// cannot be change
        
        
        final AlertDialog commitDialog = new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    dia.dismiss();
                }
            })
            .setPositiveButton("Commit", null)
            .create();
        
        amendCheckbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton v, boolean isChecked){
                if(! isChecked){
                    commitMessageEdit.setText("");
                    return;
                }
                String command = "cd " + repoPath;
                command += "\ngit log -1 --pretty=%B";
                
                new CommandTermux(command, MainActivity.this)
                    .quickSetOutputWithLoading(commitMessageEdit)
                    .setLoading(commitMessageEdit)
                    .run();
            }
        });
            
        final View.OnClickListener positiveButtonOnClick = new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String commitMessage = commitMessageEdit.getText().toString();
                if(commitMessage.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter commit message", Toast.LENGTH_SHORT).show();
                    return;
                }

                String command = "cd " + repoPath;
                command += "\ngit add .";
                command += "\ngit commit -m '"+commitMessage+"'";

                new CommandTermux(command, MainActivity.this)
                    .setOnEnd(new Runnable(){
                        @Override
                        public void run(){
                            String output = CommandTermux.getOutput();

                            if(output.contains("fatal: unable to auto-detect")){
                                Toast.makeText(MainActivity.this, "configure user name and user email first:D", Toast.LENGTH_SHORT).show();
                                configDialog = makeConfigDialog(repoPath);
                                configDialog.show();
                                return;
                            }

                            MainActivity.this.onStop();// set for refreshing
                            MainActivity.this.onResume();// refresh status
                            commitDialog.dismiss();
                        }
                    })
                    .run();
            }
        };
        
        commitDialog.setOnShowListener(new DialogInterface.OnShowListener(){
            @Override
            public void onShow(final DialogInterface dia){
                commitDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(positiveButtonOnClick);
            }
        });
        return commitDialog;
    }
    
    public class CommitChangesAdapter extends ArrayAdapter<String> {
        
        String repoPath = "";
        
        public CommitChangesAdapter(Context context, String repoPath, String[] changes) {
            super(context, 0, changes);
            this.repoPath = repoPath;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textview;

            if (convertView != null) return (TextView) convertView;
            
            textview = new TextView(getContext());
            textview.setPadding(32, 32, 32, 32);
            textview.setTypeface(Typeface.MONOSPACE);

            String item = getItem(position);

            if(item == null && item.isEmpty()) return textview;
            
            String fileStateString = item.substring(0, 2).trim();
            final char fileState = fileStateString.charAt(0);
            final String filePath = item.substring(2);
            
            String htmlString = "";
            
            switch (fileState) {
                case 'M':
                    htmlString = "<font color='#0C4EA2'>M</font> " + filePath;
                    break;
                case '?':
                    htmlString = "<font color='#20883D'>+</font> " + filePath;
                    break;
                case 'D':
                    htmlString = "<font color='#20883D'>-</font> " + filePath;
                    break;
                default:
                    htmlString = filePath;
                    break;
            }

            textview.setText(Html.fromHtml(htmlString));
            textview.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v){
                        diffDialog = makeDiffDialog(repoPath, filePath);
                        diffDialog.show();
                    }
                });

            return textview;
        }
    }
    
    AlertDialog makeDiffDialog(final String repoPath, final String filePath){
        String title = "Diff";
        ScrollView scrollView = new ScrollView(this);
        final TextView textview = new TextView(this);
        
        scrollView.addView(textview);
        textview.setTextSize(14);
        textview.setTypeface(Typeface.MONOSPACE);
        textview.setTextIsSelectable(true);

        String command = "cd " + repoPath;
        command += "\ngit diff HEAD -- " + filePath + " | sed -n '/^@@/,$p'";
        
        new CommandTermux(command, MainActivity.this)
            .setOnEnd(new Runnable(){
                @Override
                public void run(){
                    String output = CommandTermux.getOutput();
                    
                    String htmlString = "";
                    
                    for(String line : output.trim().split("\n")){
                        String backgroundColor = "";
                        if(line.startsWith("@@")) backgroundColor = "#DDF3FE";
                        if(line.startsWith("+")) backgroundColor = "#DAFAE2";
                        if(line.startsWith("-")) backgroundColor = "#FFEBEA";
                        
                        if(backgroundColor.isEmpty()){
                            htmlString += line;
                            htmlString += "<br>";
                            continue;
                        }
                        
                        htmlString += "<span style=\"background-color: "+backgroundColor+";\">"+line+"</span>";
                        htmlString += "<br>";
                    }
                    
                    textview.setText(Html.fromHtml(htmlString.trim()));
                }
            })
            .setLoading(textview)
            .run();
        return new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .create();
    }
    
    AlertDialog makeConfigDialog(final String repoPath){
        String title = "Configure Repository";
        LinearLayout layout = new LinearLayout(this);
        final EditText usernameEdit = new EditText(this);
        final EditText emailEdit = new EditText(this);
        
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(usernameEdit);
        layout.addView(emailEdit);
        usernameEdit.setHint("user name...");
        emailEdit.setHint("user email...");

        return new AlertDialog.Builder(MainActivity.this)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    String username = usernameEdit.getText().toString();
                    String email = emailEdit.getText().toString();
                    String command = "cd " + repoPath;
                    command += "\ngit config user.name \""+username+"\"";
                    command += "\ngit config user.email \""+email+"\"";
                    
                    new CommandTermux(command, MainActivity.this).run();
                    dia.dismiss();
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
