var Sequelize = require('sequelize');

function create(url) {
    var sq = new Sequelize(url);
    
    var User = sq.define('User', {
        username: {
            type: Sequelize.STRING,
            primaryKey: true,
        },
        email: { type: Sequelize.STRING },
        password: { type: Sequelize.STRING },
    })
    
    var Area = sq.define('Area', {
        title: { type: Sequelize.STRING },
        distance: { type: Sequelize.INTEGER },
    })
    Area.belongsTo(User);
    
    var AreaPoint = sq.define('AreaPoint', {
        sequence: { type: Sequelize.INTEGER },
        lat: { type: Sequelize.DOUBLE },
        lng: { type: Sequelize.DOUBLE },
    })
    AreaPoint.belongsTo(Area);
    
    var CapturePoint = sq.define('CapturePoint', {
        lat: { type: Sequelize.DOUBLE },
        lng: { type: Sequelize.DOUBLE },
    })
    CapturePoint.belongsTo(User);
    
    return sq.sync().then(_ => {
        return {
            User: User,
            Area: Area,
            AreaPoint: AreaPoint,
            CapturePoint: CapturePoint,
        }
    });
}

module.exports = create;
