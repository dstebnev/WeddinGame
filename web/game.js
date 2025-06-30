const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');

let width = window.innerWidth;
let height = window.innerHeight;
canvas.width = width;
canvas.height = height;

const assetsPath = 'assets/';

// Images
const images = {
    background: loadImage('background.png'),
    player: loadImage('player.png'),
    obstacles: [1,2,3].map(i => loadImage(`obstacle_${i}.webp`)),
    bonuses: [1,2,3,4,5,6,7,8,9].map(i => loadImage(`bonus_${i}.webp`)),
    iconStar: loadImage('ic_star.webp'),
    iconClock: loadImage('ic_clock.webp'),
};

function loadImage(name){
    const img = new Image();
    img.src = assetsPath + name;
    return img;
}

// Game variables
let gameStarted = false;
let isGameOver = false;
let hasWon = false;
let startTime = 0;
const gameDuration = 30000; // 30s
let score = 0;
let difficulty = 1; // multiplier for obstacle speed and spawn rate
const difficultyIncrease = 0.00005;

// Scoreboard
const scoreboardKey = 'scoreboard';
let scoreboard = JSON.parse(localStorage.getItem(scoreboardKey) || '[]');
scoreboard.sort((a,b)=>b.score - a.score);
let currentUser = '';
let resultSaved = false;

const usernameOverlay = document.getElementById('usernameOverlay');
const usernameInput = document.getElementById('usernameInput');
const startButton = document.getElementById('startButton');
const scoreTableBody = document.querySelector('#scoreTable tbody');
const scoreboardDiv = document.getElementById('scoreboard');

function updateScoreboard(){
    scoreboard.sort((a,b)=>b.score - a.score);
    scoreTableBody.innerHTML = '';
    scoreboard.forEach(entry=>{
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${entry.name}</td><td>${entry.score}</td>`;
        scoreTableBody.appendChild(tr);
    });
}

function saveResult(){
    scoreboard.push({name:currentUser, score});
    scoreboard.sort((a,b)=>b.score - a.score);
    localStorage.setItem(scoreboardKey, JSON.stringify(scoreboard));
    updateScoreboard();
}

updateScoreboard();

startButton.addEventListener('click', ()=>{
    const name = usernameInput.value.trim();
    if(name){
        currentUser = name;
        usernameOverlay.style.display = 'none';
        startGame();
    }
});

// Background scrolling
let bgX1 = 0;
let bgX2 = width;
const bgSpeed = 5;

// Player
const player = {
    x: 100,
    y: height * 0.8,
    width: 150,
    height: 150,
    yVelocity: 0,
    gravity: 1.2,
    jumpStrength: -25,
    isJumping: false,
    update(){
        this.y += this.yVelocity;
        if(this.y < floorY){
            this.yVelocity += this.gravity;
        } else {
            this.y = floorY;
            this.yVelocity = 0;
            this.isJumping = false;
        }
    },
    jump(){
        if(!this.isJumping){
            this.yVelocity = this.jumpStrength;
            this.isJumping = true;
        }
    },
    draw(){
        ctx.drawImage(images.player, this.x, this.y - this.height+20, this.width, this.height);
    },
    rect(){
        return {left:this.x+20, top:this.y-this.height+20, right:this.x+this.width-20, bottom:this.y-20};
    }
};

const floorY = player.y;

// Entities
const obstacles = [];
const bonuses = [];
let obstacleTimer = 0;
let bonusTimer = 0;
let nextObstacleTime = randRange(40,80);
let nextBonusTime = randRange(110,140);

function createObstacle(){
    const img = images.obstacles[Math.floor(Math.random()*images.obstacles.length)];
    obstacles.push({
        x: width,
        y: floorY,
        img,
        width:80,
        height:80,
        speed:10,
        update(){this.x -= this.speed * difficulty;},
        draw(){ctx.drawImage(img, this.x, this.y - this.height, this.width, this.height);},
        rect(){return {left:this.x+20,top:this.y-this.height+20,right:this.x+this.width-20,bottom:this.y-20};}
    });
}

function createBonus(){
    const img = images.bonuses[Math.floor(Math.random()*images.bonuses.length)];
    bonuses.push({
        x: width,
        y: floorY - 200,
        img,
        width:60,
        height:60,
        speed:10,
        update(){this.x -= this.speed * difficulty;},
        draw(){ctx.drawImage(img, this.x, this.y - this.height, this.width, this.height);},
        rect(){return {left:this.x+10,top:this.y-this.height+10,right:this.x+this.width-10,bottom:this.y-10};}
    });
}

function randRange(min,max){return Math.floor(Math.random()*(max-min))+min;}

function intersects(r1,r2){
    return !(r2.left>r1.right||r2.right<r1.left||r2.top>r1.bottom||r2.bottom<r1.top);
}

function startGame(){
    if(!currentUser){
        usernameOverlay.style.display = 'flex';
        return;
    }
    scoreboardDiv.style.display = 'none';
    gameStarted = true;
    isGameOver = false;
    hasWon = false;
    score = 0;
    resultSaved = false;
    difficulty = 1;
    obstacleTimer = 0;
    bonusTimer = 0;
    nextObstacleTime = randRange(40,80);
    nextBonusTime = randRange(110,140);
    obstacles.length=0;
    bonuses.length=0;
    startTime = performance.now();
}

function update(){
    if(!gameStarted || isGameOver) return;

    const now = performance.now();
    if(now - startTime >= gameDuration){
        isGameOver = true;
        hasWon = score >= 50;
    }

    difficulty += difficultyIncrease;

    // background
    bgX1 -= bgSpeed;
    bgX2 -= bgSpeed;
    if(bgX1 + width < 0) bgX1 = bgX2 + width;
    if(bgX2 + width < 0) bgX2 = bgX1 + width;

    // spawn obstacles/bonuses
    obstacleTimer++;
    if(obstacleTimer >= nextObstacleTime){
        createObstacle();
        obstacleTimer = 0;
        nextObstacleTime = Math.max(20, randRange(40,80) / difficulty);
    }
    bonusTimer++;
    if(bonusTimer >= nextBonusTime){
        if(Math.random() < 0.95) createBonus();
        bonusTimer = 0;
        nextBonusTime = randRange(110,140);
    }

    player.update();
    obstacles.forEach(o=>o.update());
    bonuses.forEach(b=>b.update());

    // collisions
    obstacles.forEach(o=>{
        if(intersects(player.rect(), o.rect())){
            isGameOver = true;
        }
    });
    bonuses.forEach((b,i)=>{
        if(intersects(player.rect(), b.rect())){
            bonuses.splice(i,1);
            score += 5;
            if(score>=50){
                hasWon = true;
                isGameOver = true;
            }
        }
    });

    // remove off screen
    for(let i=obstacles.length-1;i>=0;i--){
        if(obstacles[i].x + obstacles[i].width < 0) obstacles.splice(i,1);
    }
    for(let i=bonuses.length-1;i>=0;i--){
        if(bonuses[i].x + bonuses[i].width < 0) bonuses.splice(i,1);
    }
}

function draw(){
    scoreboardDiv.style.display = (!gameStarted || isGameOver) ? 'block' : 'none';
    ctx.clearRect(0,0,width,height);

    // background
    ctx.drawImage(images.background, bgX1, 0, width, height);
    ctx.drawImage(images.background, bgX2, 0, width, height);

    if(!gameStarted){
        ctx.fillStyle = 'white';
        ctx.font = '48px sans-serif';
        const title = 'Свадьба Ксю и Дани';
        const titleW = ctx.measureText(title).width;
        ctx.fillText(title, (width-titleW)/2, height/2 - 60);
        ctx.font = '32px sans-serif';
        const prompt = 'Нажми большой Enter для начала игры';
        const pW = ctx.measureText(prompt).width;
        ctx.fillText(prompt,(width-pW)/2, height/2);
        return;
    }

    player.draw();
    obstacles.forEach(o=>o.draw());
    bonuses.forEach(b=>b.draw());

    // UI
    // score block
    ctx.fillStyle = 'rgba(0,0,0,0.7)';
    const blockW = 150;
    const blockH = 60;
    ctx.fillRect(width - blockW - 20, 20, blockW, blockH);
    ctx.drawImage(images.iconStar, width - blockW - 10, 25, 40,40);
    ctx.fillStyle = 'white';
    ctx.font = '32px sans-serif';
    ctx.fillText(score.toString().padStart(2,'0'), width - blockW +40, 60);

    // timer block
    const remaining = Math.max(0, Math.floor((gameDuration - (performance.now()-startTime))/1000));
    ctx.fillStyle = 'rgba(255,215,0,0.7)';
    ctx.fillRect(20, 20, blockW, blockH);
    ctx.drawImage(images.iconClock, 25,25,40,40);
    ctx.fillStyle='black';
    ctx.fillText(remaining.toString().padStart(2,'0'), 70, 60);

    if(isGameOver){
        if(!resultSaved){
            saveResult();
            resultSaved = true;
        }
        ctx.fillStyle='white';
        ctx.font = '64px sans-serif';
        const text = hasWon ? 'Победа!' : 'Game Over!';
        const tW = ctx.measureText(text).width;
        ctx.fillText(text, (width-tW)/2, height/2 - 40);
        ctx.font='40px sans-serif';
        const scoreText = 'Баллы: ' + score;
        const stW = ctx.measureText(scoreText).width;
        ctx.fillText(scoreText, (width-stW)/2, height/2+10);
        ctx.font='32px sans-serif';
        const restartText1='Shift - снова играть';
        const restartText2='Tab - новый игрок';
        const r1W=ctx.measureText(restartText1).width;
        const r2W=ctx.measureText(restartText2).width;
        ctx.fillText(restartText1, (width-r1W)/2, height/2+60);
        ctx.fillText(restartText2, (width-r2W)/2, height/2+100);
    }
}

function gameLoop(){
    update();
    draw();
    requestAnimationFrame(gameLoop);
}

// Events
window.addEventListener('keydown', e=>{
    if(e.code==='Space' || e.code==='Enter'){
        if(!gameStarted) {
            startGame();
        } else if(!isGameOver) {
            player.jump();
        }
    } else if(isGameOver && (e.code==='ShiftLeft' || e.code==='ShiftRight')){
        startGame();
    } else if(isGameOver && e.code==='Tab'){
        e.preventDefault();
        currentUser='';
        usernameOverlay.style.display='flex';
        gameStarted=false;
    }
});

canvas.addEventListener('mousedown', ()=>{
    if(!gameStarted) startGame();
    else if(isGameOver) startGame();
    else player.jump();
});

window.addEventListener('resize', ()=>{
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = width;
    canvas.height = height;
    bgX1 = 0;
    bgX2 = width;
    player.y = height*0.8;
});

// Start
requestAnimationFrame(gameLoop);