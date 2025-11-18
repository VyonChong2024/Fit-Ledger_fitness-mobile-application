CREATE TABLE IF NOT EXISTS  User  (
	 User_ID 	TEXT PRIMARY KEY,
	 Gender 	TEXT,
	 Age 	INTEGER,
	 Height 	REAL,
	 Weight 	REAL,
	 BodyFatPercent 	REAL,
	 TargetBodyFat 	REAL,
	 TargetWeight 	REAL,
	 DietPlan 	TEXT,
	 isSynced INTEGER DEFAULT 0
);



CREATE TABLE IF NOT EXISTS  Exercise  (
	 Exercise_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 Name 	TEXT,
	 Instruction 	TEXT,
	 Category 	TEXT,
	 MuscleGroup 	TEXT,
	 EquipmentUsed 	TEXT,
	 Gif_URL 	TEXT
);



CREATE TABLE IF NOT EXISTS Food (
    Food_ID INTEGER PRIMARY KEY AUTOINCREMENT,
    Food_Name TEXT,
    Calories NUMERIC,
    Protein NUMERIC,
    Carbohydrates NUMERIC,
    Fat NUMERIC,
    Iron NUMERIC,
    Calcium NUMERIC,
    Potassium NUMERIC,
    Magnesium NUMERIC,
    Zinc NUMERIC,
    Sodium NUMERIC,
    VitaminD NUMERIC,
    VitaminA NUMERIC,
    VitaminC NUMERIC,
    VitaminK NUMERIC,
    VitaminB12 NUMERIC,
    Category TEXT
);

CREATE TABLE IF NOT EXISTS FoodPortion (
    FoodPortion_ID INTEGER PRIMARY KEY AUTOINCREMENT,
    Food_ID INTEGER,
    Unit TEXT,
    UnitValue TEXT
);



CREATE TABLE IF NOT EXISTS  WorkoutLog  (
	 Log_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 User_ID 	TEXT,
	 Date 	TEXT,
	 StartTime 	NUMERIC,
	 Duration 	NUMERIC,
	 Notes 	TEXT,
	 isSynced INTEGER DEFAULT 0,
	FOREIGN KEY( User_ID ) REFERENCES  User ( User_ID )
);

CREATE TABLE IF NOT EXISTS  WorkoutExercise  (
	 WorkoutExercise_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 Log_ID 	INTEGER,
	 Exercise_ID 	INTEGER,
	 isSynced INTEGER DEFAULT 0,
	FOREIGN KEY( Exercise_ID ) REFERENCES  Exercise ( Exercise_ID ),
	FOREIGN KEY( Log_ID ) REFERENCES  WorkoutLog ( Log_ID )
);

CREATE TABLE IF NOT EXISTS  ExerciseSet  (
	 ExerciseSet_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 WorkoutExercise_ID 	INTEGER,
	 Set_No 	TEXT,
	 Reps 	INTEGER,
	 WeightUsed 	REAL,
	 isSynced INTEGER DEFAULT 0,
	FOREIGN KEY( WorkoutExercise_ID ) REFERENCES  WorkoutExercise ( WorkoutExercise_ID )
);



CREATE TABLE IF NOT EXISTS WorkoutPlan (
    Plan_ID INTEGER PRIMARY KEY AUTOINCREMENT,
    User_ID TEXT,
    PlanName TEXT,
    CreatedDate TEXT,
	isSynced INTEGER DEFAULT 0,
    FOREIGN KEY(User_ID) REFERENCES User(User_ID)
);

CREATE TABLE IF NOT EXISTS WorkoutPlanDay (
    PlanDay_ID INTEGER PRIMARY KEY AUTOINCREMENT,
    Plan_ID INTEGER,
    DayName TEXT,
    WorkoutName TEXT,
	isSynced INTEGER DEFAULT 0,
    FOREIGN KEY(Plan_ID) REFERENCES WorkoutPlan(Plan_ID)
);

CREATE TABLE IF NOT EXISTS WorkoutPlanExercise (
    PlanExercise_ID INTEGER PRIMARY KEY AUTOINCREMENT,
    PlanDay_ID INTEGER,
    ExerciseName TEXT,
    Sets INTEGER,
    Reps INTEGER,
	isSynced INTEGER DEFAULT 0,
    FOREIGN KEY(PlanDay_ID) REFERENCES WorkoutPlanDay(PlanDay_ID)
);



CREATE TABLE IF NOT EXISTS  MealLog  (
	 Log_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 User_ID 	TEXT,
	 Date 	TEXT,
	 Time 	TEXT,
	 Notes 	TEXT,
	 isSynced INTEGER DEFAULT 0,
	FOREIGN KEY( User_ID ) REFERENCES  User ( User_ID )
);

CREATE TABLE IF NOT EXISTS  MealLogFood  (
	 MealLog_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 Log_ID 	INTEGER,
	 Food 	TEXT,
	 Quantity 	NUMERIC,
	 isSynced INTEGER DEFAULT 0,
	FOREIGN KEY( Log_ID ) REFERENCES  MealLog ( Log_ID )
);



CREATE TABLE IF NOT EXISTS NutrientRequirement (
    NutrietReq_ID INTEGER PRIMARY KEY AUTOINCREMENT,
    User_ID TEXT,
    Calories REAL,
    Protein REAL,
    Carbohydrates REAL,
    Fat REAL,
    Iron REAL,
    Calcium REAL,
    Potassium REAL,
    Magnesium REAL,
    Zinc REAL,
    Sodium REAL,
    VitaminD REAL,
    VitaminA REAL,
    VitaminC REAL,
    VitaminK REAL,
    VitaminB12 REAL,
	isSynced INTEGER DEFAULT 0,
    FOREIGN KEY( User_ID ) REFERENCES  User ( User_ID )
);



CREATE TABLE IF NOT EXISTS  WeightHistory  (
	 Weight_His_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 User_ID 	TEXT,
	 Weight 	REAL,
	 Date 	TEXT,
	 isSynced INTEGER DEFAULT 0,
	FOREIGN KEY( User_ID ) REFERENCES  User ( User_ID )
);

CREATE TABLE IF NOT EXISTS  BodyFatHistory  (
	 BodyFat_His_ID 	INTEGER PRIMARY KEY AUTOINCREMENT,
	 User_ID 	TEXT,
	 BodyFatPercent 	REAL,
	 Date 	TEXT,
	 isSynced INTEGER DEFAULT 0,
	FOREIGN KEY( User_ID ) REFERENCES  User ( User_ID )
);




