use std::sync::Mutex;
use tauri::Manager;

struct OpenedUrls(Mutex<Vec<String>>);

#[tauri::command]
fn opened_urls(state: tauri::State<'_, OpenedUrls>) -> Vec<String> {
    state.0.lock().unwrap().clone()
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .manage(OpenedUrls(Mutex::new(vec![])))
        .invoke_handler(tauri::generate_handler![opened_urls])
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_dialog::init())
        .build(tauri::generate_context!())
        .expect("error while running tauri application")
        .run(|app, event| {
            #[cfg(target_os = "macos")]
            if let tauri::RunEvent::Opened { urls } = event {
                use tauri::Emitter;
                let strings: Vec<String> = urls.iter().map(|u| u.to_string()).collect();
                app.state::<OpenedUrls>().0.lock().unwrap().extend(strings.clone());
                let _ = app.emit("opened", strings);
            }
        });
}
