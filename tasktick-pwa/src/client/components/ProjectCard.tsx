import * as React from 'react';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import classnames from 'classnames';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import withRoot from '../withRoot';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardMedia from '@material-ui/core/CardMedia';
import CardContent from '@material-ui/core/CardContent';
import CardActions from '@material-ui/core/CardActions';
import Collapse from '@material-ui/core/Collapse';
import Avatar from '@material-ui/core/Avatar';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import red from '@material-ui/core/colors/red';
import FavoriteIcon from '@material-ui/icons/Favorite';
import ShareIcon from '@material-ui/icons/Share';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import MoreVertIcon from '@material-ui/icons/MoreVert';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import CardActionArea from '@material-ui/core/CardActionArea';
import { Project, Task } from '../stores/data';

const styles = (theme: Theme) =>
  createStyles({
    card: {
        maxWidth: 800,
      },
      media: {
        height: 0,
        paddingTop: '56.25%', // 16:9
      },
      actions: {
        display: 'flex',
      },
      expand: {
        transform: 'rotate(0deg)',
        transition: theme.transitions.create('transform', {
          duration: theme.transitions.duration.shortest,
        }),
        marginLeft: 'auto',
        [theme.breakpoints.up('sm')]: {
          marginRight: -8,
        },
      },
      expandOpen: {
        transform: 'rotate(180deg)',
      },
      avatar: {
        backgroundColor: red[500],
      },
  });

type State = {
    expanded: boolean
};

interface Props {
  store: any;
  project: Project;  
  expanded?: boolean;
  onTaskSelect: (Task) => void;
  onProjectSelect?: (Project) => void;
};

class ProjectCard extends React.Component<Props & WithStyles<typeof styles>, State> {
    state = { expanded: this.props.expanded };

    handleExpandClick = () => {
      this.setState(state => ({ expanded: !state.expanded }));
    };
    onTaskSelect = (x: Task) => {
        console.log("onTaskSelect")
        this.props.onTaskSelect(x);
    }
    selectServiceType = () =>{
        if(this.props.project)
            this.props.onProjectSelect(this.props.project)
    }

  render() {    
    const classes = this.props.classes 
    const sd = this.props.project
    return (
        <Card className={classes.card}>          
        <CardHeader
          avatar={
            <Avatar aria-label="Service" className={classes.avatar}>
              S
            </Avatar>
          }
          action={
            <IconButton>
              <MoreVertIcon />
            </IconButton>
          }
          title={sd.name}
          subheader={sd.description}
        />
        <CardActionArea onClick={this.selectServiceType}>
        <CardMedia
          className={classes.media}
          image={"/imgs/"+sd.name+".png"}
          title={sd.name}
        />
        <CardContent>
          <Typography component="p">
            {sd.description}            
          </Typography>
        </CardContent>
        </CardActionArea>
        <CardActions className={classes.actions} disableActionSpacing>
          <IconButton aria-label="Add to favorites">
            <FavoriteIcon />
          </IconButton>        
          <IconButton
            className={classnames(classes.expand, {
              [classes.expandOpen]: this.state.expanded,
            })}
            onClick={this.handleExpandClick}
            aria-expanded={this.state.expanded}
            aria-label="Show more"
          >
            <ExpandMoreIcon />
          </IconButton>
        </CardActions>
        <Collapse in={this.state.expanded} timeout="auto" unmountOnExit>
          <CardContent>
            <Typography paragraph>Methods:</Typography>
            <Typography paragraph>
              Click on a method to trigger an event of that type:
            </Typography>
            <div>
                <List>
                { sd.tasks.map(x => this.props.store.taskStore.tasks[x] ).map( (x: Task ) => (
                    <ListItem key={x.id} button={true} onClick={() => this.onTaskSelect(x) } >
                        <ListItemText                      
                        primary={x.name}
                        secondary={x.section}
                        />
                    </ListItem>)                
                )}
              </List> 
            </div>
          </CardContent>
        </Collapse>
      </Card>
    );
  }
}

export default withRoot(withStyles(styles)(ProjectCard));