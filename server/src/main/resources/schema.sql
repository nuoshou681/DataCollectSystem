CREATE TABLE IF NOT EXISTS crawler_page_result (
    page_result_id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    node_id VARCHAR(100) NULL,
    page_url TEXT NOT NULL,
    page_title VARCHAR(500) NULL,
    page_index INT NOT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    file_path VARCHAR(1024) NULL,
    error_message TEXT NULL,
    mhtml_cached TINYINT(1) NOT NULL DEFAULT 0,
    mhtml_cached_at DATETIME NULL,
    mhtml_content LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (page_result_id),
    UNIQUE KEY uk_task_page (task_id, page_index, page_url(255)),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
