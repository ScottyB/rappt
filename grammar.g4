grammar AML;

tokens{LetterOrDigit}

parse
    : (app | screen | api | tracker | theme)* EOF
    ;

app
    : 'app' ('{' appProperties+ '}')?
    ;

appProperties
    : ANDROID_SDK STRING
    | menu
    | navigation
    | acra
    | LANDING_PAGE '=' variable
    | MAP_KEY '=' STRING
    ;

acra : 'acra' STRING ;

variableDeclaration
    : ID
    ;

variable
    : ID
    ;

screen
    : 'screen' variableDeclaration ('(' screenParams ')')?  ('{' screenProperties+ '}')?
    ;

screenParams
    : STRING_VAR variableDeclaration
    ;

screenProperties
    : FEATURES idArray
    | BACK BOOL
    | action
    // | PULL_TO_REFRESH BOOL
    | 'controller' behaviours
    | layout
    | screenView
    | screenModel
    | TITLE '=' STRING
    ;

screenView
    : 'view' '{' 'group' uiBlock '}'
    ;

uiBlock
    :  variableDeclaration STRING? ('{' viewProperties* '}')?
    ;

screenModel
    : 'model' '{' source+ '}'
    ;

source
    : variableDeclaration '=' 'source' '(' sourceProperties (',' sourceProperties)*  ')' sourceBlock?
    ;

sourceBlock
    : '{' sourceExtras*  '}'
    ;

sourceExtras
    : variable (BIND_FROM | BIND_TO) field
    ;

sourceProperties
    : END_POINT ':' variable '.' variable
    ;

behaviours
    : '{' behaviourProperties+ '}'
    ;

behaviourProperties
    : ON_CLICK variable instructionBlock
    ;

menu
    : 'menu' variableDeclaration '{' action+ '}'
    ;

instructionBlock
    : '{' (instructions+ (CLOSE BOOL)?)? '}'
    ;

instructions
    : to
    | toUrl
    | call
    | removePreference
    | getPreference
    | showToast
    | notification
    | currentLocation
    ;

to
    : TO variable ('(' variable ':' field ')')?
    ;

toUrl: TO STRING;

call
    : 'call' variable '.' variable
    | 'call' variable '.' variable PASSED ID
    ;

removePreference
    : REMOVE variable
    ;

getPreference
    : GET variable 'for' variable
    ;

action
    : 'action' variableDeclaration STRING STRING? instructionBlock?
    ;

resource
    : variableDeclaration '=' HTTP_METHOD '(' resourceProperties (',' resourceProperties)* ')'
    | HTTP_METHOD INIT variableDeclaration STRING field STRING ('{' resourceProperties+ '}')?
    ;

resourceProperties
    : STATE_FIELD field
    | RETURNS_LIST ':' BOOL
    | PUT variableDeclaration field
    | END_POINT ':' STRING
    ;

field
    : fieldValue ('.' fieldValue)*
    ;

fieldValue
    : ID FIELD_TYPE?
    ;

navigation
    : NAV_TYPE variableDeclaration ('{' navigationProperties+ '}')
    ;

tab : 'tab' variableDeclaration STRING to ;

navigationProperties
    // : DISABLE_SWIPE ':' BOOL
    // | SUB_HEADER ':' BOOL
    : tab
    ;

idArray
    : variable (variable)*
    ;

api
    : 'api' variableDeclaration ('{' apiProperties+ '}')?
    ;

apiProperties
    : MOCK_DATA BOOL
    | END_POINTS '{' resource+ '}'
    | BASE '=' STRING
    | OAUTH '{' oauthProperties+ '}'
    | AUTH '(' 'type' ':' TOKEN ')' '{' authProperties+ '}'
    | AUTH '(' 'type' ':' PARSE ')' '{' parseProps+ '}'
    ;

parseProps
    : CLIENT_KEY '=' STRING
    | APP_ID '=' STRING
    ;

PARSE : 'parse' ;
CLIENT_KEY : 'clientKey' ;
APP_ID : 'appId' ;

authProperties
    : API_KEY '=' STRING
    | TOKEN_PAR '=' STRING
    ;

oauthProperties
    : API_PROVIDER STRING
    | API_KEY STRING
    | API_SECRET STRING
    | API_VERIFIER_PARAMETER STRING
    | CALLBACK STRING
    ;

viewProperties
    : styleReference? (button | label | textInput | image )
    | map
    | list
    | web
    | layout
    ;

layout
    : 'layout' '{' layout_spec '}'
    ;

layout_spec
    : ('>' '|'* ID* ('|' ID)*)*
    ;

map
    : 'map' variableDeclaration '{' mapProperties+ '}'
    ;

mapProperties
    : marker
    | polyline
    | NO_INTERACTIONS
    | ON_MAP_CLICK instructionBlock?
    ;

marker
    : variableDeclaration '=' 'marker' '(' markerProperties (',' markerProperties)+ ')'
    ;

markerProperties
    : TEXT (STRING | field)
    | TITLE ':' (STRING | field)
    | LAT ':' (NUMBER | field)
    | LONG ':' (NUMBER | field)
    ;

currentLocation
    : CURRENT_LOCATION ID
    ;

notification
    :'notification' variableDeclaration STRING STRING STRING ( '{' notificationBlock '}')?
    |'notification' variableDeclaration STRING field field ( '{' notificationBlock '}')?
    ;

notificationBlock
    // : BIG_TEXT (STRING | field)
    // | BIG_PICTURE (STRING | field)
    // | INBOX (STRING | field)
    :
    | TO variable
    ;

polyline
    : 'polyline' variable variable ;

// TODO: ID is a bug and needs to be a variableDeclaration
data
    : 'data' variableDeclaration '{' ID '}' STRING*
    ;

// UI Elements
list
    : 'list' variableDeclaration '{' listProperties+ '}'
    ;

listProperties
    : data
    | ON_ITEM_CLICK instructionBlock?
    | 'row' uiBlock
    ;


button
    : variableDeclaration '=' 'button' '(' labelProperties* ')'
    ;

label
    : variableDeclaration '=' 'label' '(' labelProperties* ')'
    | 'label' PASSED variable
    ;

labelProperties
    : TEXT STRING
    | BINDING ':' variable
    ;

image
    : variableDeclaration '=' 'image' '(' imageProperties* ')'
    ;

imageProperties
    : FILE ':' STRING
    | BINDING ':' field
    ;

web
    : 'web' variableDeclaration STRING
    | 'web' variableDeclaration 'authenticate' ID 'to' ID
    ;

textInput
    : variableDeclaration '=' ('text-input' | 'input') '(' textInputProperties* ')'
    ;

textInputProperties
    : HINT ':' STRING
    | BINDING ':' field
    ;

showToast : 'toast' variableDeclaration STRING ;

// Styles and themes
styleReference
    : '@style(' variable  ')' | HEADING | CAPTION | BODY
    ;

style
    : 'style' variableDeclaration STRING
    ;

theme
    : 'theme' '{' (style | primary)* '}'
    ;

primary
    : PRIMARY STRING
    ;

// Tracker
tracker
    : 'tracker' '{' trackerProperties*  '}'
    ;

trackerProperties
    : REGISTER ID '.' ID
    | SESSION ID '.' ID
    | UPDATES ID '.' ID
    | TRACKING_INTERVAL NUMBER
    | track
    | ON_UPDATE instructionBlock?
    ;

track
    : TRACK_LOCATION '{' UPDATE_INTERVAL NUMBER FASTEST_INTERVAL NUMBER '}'
    | TRACK_ACTIVITY '{' UPDATE_INTERVAL NUMBER '}'
    ;


REGISTER : 'register';
SESSION : 'session';
UPDATES : 'updates';
TRACKING_INTERVAL : 'tracking-interval';
TRACK_LOCATION : 'track-location';
TRACK_ACTIVITY : 'track-activity';
UPDATE_INTERVAL : 'update-interval';
FASTEST_INTERVAL : 'fastest-interval';

PASSED : 'passed' ;

// Events
ON_CLICK : 'on-click';
ON_ITEM_CLICK : 'on-item-click';
ON_MAP_CLICK : 'on-map-click';
ON_UPDATE : 'on-update' ;

// Instructions
TO : 'to' | 'navigate-to';

// App properties
ANDROID_SDK : 'android-sdk';       // todo: deprecated
MOCK_DATA : 'mock-data';
LANDING_PAGE : 'landingPage';

// Screen properties
FEATURES : 'features';
BACK : 'back' ;
TITLE : 'title' ;

STRING_VAR : 'string';

// UI properties
TEXT : 'text' ' '* ':' ;
BINDING : 'binding';
FILE : 'file' ;
HINT : 'hint';
LAT : 'lat';
LONG : 'long';

// Navigation methods
NAV_TYPE : 'tabbar' | 'drawer';

// Button properties
CLOSE : 'close';

PRIMARY : 'primary-colour';

// Shared preferences
GET : 'get';
PUT : 'put';
REMOVE : 'remove';

NO_INTERACTIONS : 'no-interactions';

CURRENT_LOCATION : 'current-location';

INIT : 'init';
HTTP_METHOD : 'GET' | 'POST' | 'PUT' | 'DELETE' ;

// FIELD_TYPEs must start with a :
FIELD_TYPE : ':object' | ':list' | ':string' | ':email' | ':phone' | ':password' | ':image';

HEADING : '@heading';
BODY : '@body';
CAPTION : '@caption';

BIG_PICTURE : 'bigText';
BIG_TEXT : 'bigPicture';
INBOX : 'inbox';


MAP_KEY : 'mapKey';
CALLBACK : 'callback';
API_KEY : 'apiKey';
TOKEN_PAR : 'tokenParam' ;
API_SECRET :'api-secret';
OAUTH : 'oauth';
AUTH : 'auth';
TOKEN : 'token' ;
API_PROVIDER : 'api-provider';
API_VERIFIER_PARAMETER : 'api-verifier-parameter' ;
BASE : 'base' ;
END_POINTS : 'endPoints';
END_POINT : 'endPoint' ;

RETURNS_LIST: 'returnsList';
STATE_FIELD : 'state-field';
PULL_TO_REFRESH :'pull-to-refresh';

// Navigation options
DISABLE_SWIPE : 'disable-swipe';
SUB_HEADER : 'sub-header';

BOOL : 'true' | 'false' ;
WS : [ \r\t\n]+ -> skip ;
STRING :  '"' ( '\\"' | . | '-' )*? '"' ;
ID : [a-zA-Z] [a-zA-Z0-9_-]* ;
COMMENT : '/*' .*? '*/' -> skip;
SINGLE_LINE_COMMENT : '//' ~[\r\n]* -> skip ;
NUMBER : '-'?('0'..'9')+ ('.' ('0'..'9')+)?;
BIND_FROM :'<=' ;
BIND_TO    :'=>';
