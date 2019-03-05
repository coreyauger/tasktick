import * as React from 'react';
import Avatar from '@material-ui/core/Avatar';
import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import FormControl from '@material-ui/core/FormControl';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import Input from '@material-ui/core/Input';
import InputLabel from '@material-ui/core/InputLabel';
import LockIcon from '@material-ui/icons/LockOutlined';
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import withRoot from '../withRoot';
import { Link } from '@material-ui/core';

const styles = (theme: Theme) =>
createStyles({
  layout: {
    width: 'auto',
    display: 'block', // Fix IE 11 issue.
    marginLeft: theme.spacing.unit * 3,
    marginRight: theme.spacing.unit * 3,
    [theme.breakpoints.up(400 + theme.spacing.unit * 3 * 2)]: {
      width: 400,
      marginLeft: 'auto',
      marginRight: 'auto',
    },
  },
  whiteout: {
    position: "static",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    width: "100%",
    height: "100%",
    color: "white"
  },
  paper: {
    marginTop: theme.spacing.unit * 8,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: `${theme.spacing.unit * 2}px ${theme.spacing.unit * 3}px ${theme.spacing.unit * 3}px`,
  },
  avatar: {
    margin: theme.spacing.unit,
    backgroundColor: theme.palette.secondary.main,
  },
  form: {
    width: '100%', // Fix IE 11 issue.
    marginTop: theme.spacing.unit,
  },
  submit: {
    marginTop: theme.spacing.unit * 3,
  },
});

type State = {
  email: string,
  name: string,
  password: string
  mode: string
};

interface Props {
  store: any;
};

class SignIn extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = { name: "", email: "", password: "", mode: "login" };


  
  doSignIn = (event) => {   
    const userAction = async () => {
      const response = await fetch('/api/user/login', {
        method: 'POST',
        body: JSON.stringify({email: this.state.email, password: this.state.password }), // string or object
        headers:{
          'Content-Type': 'application/json'
        }
      });
      const json = await response.json(); //extract JSON from the http response
      console.log("ret", json)
      window.localStorage.setItem('authToken', json.authToken)
      window.localStorage.setItem('refreshToken', json.refreshToken)
      // do something with myJson
    }
    userAction()
    event.preventDefault();    
  }
  doRegister = (event) => {   
    const userAction = async () => {
      const response = await fetch('/api/user/register', {
        method: 'POST',
        body: JSON.stringify({email: this.state.email, password: this.state.password, firstName: this.state.name, lastName: this.state.name }), // string or object
        headers:{
          'Content-Type': 'application/json'
        }
      });
      const json = await response.json(); //extract JSON from the http response
      console.log("ret", json)
      window.localStorage.setItem('authToken', json.authToken)
      window.localStorage.setItem('refreshToken', json.refreshToken)      
    }
    userAction()
    event.preventDefault();    
  }
  handlePasswordChange =  (event) => {
    this.setState({ password: event.target.value });
  };
  handleEmailChange =  (event) => {
    this.setState({ email: event.target.value });
  };
  handleNameChange =  (event) => {
    this.setState({ name: event.target.value });
  };
  setMode = (mode: string) => () => {
    this.setState({mode: "register"})
  }

  render() {  
    const  classes = this.props.classes;
    return (
      <React.Fragment>
        <CssBaseline />
        <div className={classes.whiteout}>
        <main className={classes.layout}>
          {this.state.mode == "login" ?
          <Paper className={classes.paper}>
            <Avatar className={classes.avatar}>
              <LockIcon />
            </Avatar>
            <Typography component="h1" variant="h5">Sign in</Typography>
            
            <form className={classes.form}>
              <FormControl margin="normal" required fullWidth>
                <InputLabel htmlFor="email">Email Address</InputLabel>
                <Input id="email" name="email" autoComplete="email" autoFocus onChange={this.handleEmailChange} />
              </FormControl>
              <FormControl margin="normal" required fullWidth>
                <InputLabel htmlFor="password">Password</InputLabel>
                <Input
                  name="password"
                  type="password"
                  id="password"
                  autoComplete="current-password"
                  onChange={this.handlePasswordChange}
                />
              </FormControl>
              <FormControlLabel control={<Checkbox value="remember" color="primary" />} label="Remember me" />
              <Button
                type="submit"
                fullWidth
                variant="contained"
                color="primary"
                className={classes.submit}
                onClick={this.doSignIn}
              >
                Sign in
              </Button>              
            </form>
            <Link onClick={this.setMode('register')}>Or Register a new account.</Link>
          </Paper>
          :
          <Paper className={classes.paper}>
            <Avatar className={classes.avatar}>
              <LockIcon />
            </Avatar>
            <Typography component="h1" variant="h5">Register</Typography>
            
            <form className={classes.form}>
            <FormControl margin="normal" required fullWidth>
                <InputLabel htmlFor="email">Full Name</InputLabel>
                <Input id="name" name="name" autoComplete="name" autoFocus onChange={this.handleNameChange} />
              </FormControl>
              <FormControl margin="normal" required fullWidth>
                <InputLabel htmlFor="email">Email Address</InputLabel>
                <Input id="email" name="email" autoComplete="email" autoFocus onChange={this.handleEmailChange} />
              </FormControl>
              <FormControl margin="normal" required fullWidth>
                <InputLabel htmlFor="password">Password</InputLabel>
                <Input
                  name="password"
                  type="password"
                  id="password"
                  autoComplete="current-password"
                  onChange={this.handlePasswordChange}
                />
              </FormControl>
              <FormControl margin="normal" required fullWidth>
                <InputLabel htmlFor="password">Confirm</InputLabel>
                <Input
                  name="password2"
                  type="password2"
                  id="password2"
                  autoComplete="current-password"
                />
              </FormControl>
              <FormControlLabel control={<Checkbox value="remember" color="primary" />} label="Remember me" />
              <Button
                type="submit"
                fullWidth
                variant="contained"
                color="primary"
                className={classes.submit}
                onClick={this.doRegister}
              >
                Register
              </Button>              
            </form>
            <Link onClick={this.setMode('login')}>Or Sign In.</Link>
          </Paper>
          }
        </main>
        </div>
      </React.Fragment>
    );
  }
}

export default withRoot(withStyles(styles)(SignIn));
