CREATE DATABASE IF NOT EXISTS flappy_bird;

USE flappy_bird;

CREATE TABLE IF NOT EXISTS scores (
    id INT PRIMARY KEY AUTO_INCREMENT,
    player_name VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_scores_ranking (score DESC, created_at ASC)
);
